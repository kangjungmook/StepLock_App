package com.steplock.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.steplock.app.MainActivity
import com.steplock.app.R
import com.steplock.app.data.DailyStat
import com.steplock.app.data.SettingsRepository
import com.steplock.app.data.SleepRepository
import com.steplock.app.data.StepTracker
import com.steplock.app.data.UnlockEvaluator
import com.steplock.app.data.hasActivityRecognitionPermission
import com.steplock.app.ui.LockActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

/**
 * 전경 앱을 1초 간격으로 확인해 차단 대상이 열리면 잠금 화면을 띄웁니다.
 * 접근성 서비스 대신 사용 정보 접근 권한을 쓰기 때문에 감지가 1초 정도 늦습니다.
 */
class AppWatchService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var shownForAppId: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startWatchNotification()
        scope.launch { watchForegroundApp() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun watchForegroundApp() {
        val usageStats = getSystemService(UsageStatsManager::class.java) ?: return
        val repository = SettingsRepository(this)
        val sleepRepository = SleepRepository(this, repository)
        val preferences = repository.preferences.stateIn(scope)
        val steps = StepTracker(this, repository).todaySteps()
            .stateIn(scope, SharingStarted.Eagerly, 0)
        var lastSleepRefreshAt = 0L
        var lastRecordAt = 0L
        // 이 서비스가 **잠긴 상태를 본 적이 있는지**. 서비스가 막 떠서 처음 본 게
        // 이미 열린 상태면 알리지 않습니다 — 재시작할 때마다 "열렸어요"가 뜨면 안 됩니다.
        var sawLocked = false
        var sawLockedOn: LocalDate? = null
        var nudgedOn: LocalDate? = null

        while (currentCoroutineContext().isActive) {
            val current = preferences.value
            val now = System.currentTimeMillis()

            // 수면은 Health Connect를 매초 읽을 수 없어 캐시를 주기적으로만 갱신합니다.
            if (current.settings.sleepEnabled && now - lastSleepRefreshAt > SLEEP_REFRESH_MS) {
                lastSleepRefreshAt = now
                sleepRepository.refresh()
            }

            val stat = DailyStat(
                deviceUuid = current.settings.deviceUuid,
                accountId = current.settings.accountId,
                date = LocalDate.now(),
                steps = steps.value,
                sleepMinutes = current.sleepMinutesToday,
                pomodoroSessions = current.pomodoro.sessionsToday,
            )

            // 하루 기록을 여기서도 남깁니다.
            //
            // 전에는 화면이 열릴 때만 남겼습니다. 그래서 **하루 종일 앱을 한 번도
            // 열지 않으면 그날 걸음이 기록되지 않고**, 자정에 걸음 기준점이 새로
            // 잡히면서 영구히 사라졌습니다 — 통계에 0보로 남고 연속 달성도 끊겼습니다.
            // 자정을 정확히 집어낼 방법이 없으니 주기적으로 덮어씁니다. 값이 그대로면
            // recordDay 가 쓰지 않습니다.
            if (now - lastRecordAt > RECORD_INTERVAL_MS) {
                lastRecordAt = now
                repository.recordDay(stat)
            }

            // 잠김 → 열림으로 바뀐 순간 한 번 알립니다. 걸음 목표를 채웠는지 알려고
            // 앱을 열어 볼 필요가 없게 합니다. 날짜가 바뀌면 다시 셉니다.
            val unlockedNow = current.settings.blockedAppIds.isNotEmpty() &&
                UnlockEvaluator.isUnlocked(current.settings, stat)
            if (sawLockedOn != stat.date) {
                sawLocked = false
                sawLockedOn = stat.date
            }
            if (!unlockedNow) {
                sawLocked = true
            } else if (sawLocked) {
                sawLocked = false
                notifyUnlocked()
            }

            // 저녁 산책 응원 — 하루 한 번, 저녁에 걸음만 모자라 잠겨 있을 때.
            // 잠긴 걸 알게 되는 건 보통 앱을 열었을 때라 이미 늦습니다. 아직 걸을
            // 시간이 남았을 때 한 번 알려 줍니다. 걸음 권한이 없으면 0보가 진짜인지
            // 알 수 없어 보내지 않습니다.
            val hour = LocalTime.now().hour
            if (nudgedOn != stat.date &&
                hour in NUDGE_HOURS &&
                !unlockedNow &&
                current.settings.blockedAppIds.isNotEmpty() &&
                current.settings.stepsEnabled &&
                stat.steps < current.settings.stepGoal &&
                hasActivityRecognitionPermission(this)
            ) {
                nudgedOn = stat.date
                notifyEveningWalk(current.settings.stepGoal - stat.steps)
            }

            // 고른 앱의 패키지 이름을 그대로 비교합니다. 예전에는 코드에 박아 둔
            // 네 개 중에서만 찾았기 때문에, 틱톡 라이트처럼 패키지가 다른 앱은
            // 골라도 걸리지 않았습니다.
            val blockedPackage = foregroundPackage(usageStats)
                ?.takeIf { it in current.settings.blockedAppIds }

            if (blockedPackage == null) {
                shownForAppId = null
            } else if (current.temporaryAllow.isActive()) {
                // 허용 중에는 표시 기록을 비워 둡니다.
                //
                // 전에는 이 기록이 그대로 남아서, **5분이 지나도 그 앱에 머물러
                // 있는 동안에는 잠금이 다시 뜨지 않았습니다** — 한 번 허용하면
                // 앱을 떠나지 않는 한 무제한으로 쓸 수 있었습니다.
                shownForAppId = null
            } else if (shownForAppId != blockedPackage) {
                // 집중 세션이 도는 동안에는 조건을 이미 채웠어도 잠급니다.
                // 그러지 않으면 타이머를 켜 둔 채 쇼츠를 봐도 "집중 1회"가 쌓여서,
                // 집중 조건이 아무것도 증명하지 못합니다.
                val focusing = current.pomodoro.isRunning
                if (focusing || !UnlockEvaluator.isUnlocked(current.settings, stat)) {
                    shownForAppId = blockedPackage
                    startActivity(LockActivity.intent(this, blockedPackage))
                    repository.recordBlockedOpen(blockedPackage)
                }
            }
            delay(POLL_INTERVAL_MS)
        }
    }

    private fun foregroundPackage(usageStats: UsageStatsManager): String? {
        val now = System.currentTimeMillis()
        val events = usageStats.queryEvents(now - EVENT_WINDOW_MS, now)
        val event = UsageEvents.Event()
        var packageName: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                packageName = event.packageName
            }
        }
        return packageName
    }

    private fun notifyUnlocked() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                UNLOCK_CHANNEL_ID,
                getString(R.string.unlock_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
        val openApp = PendingIntent.getActivity(
            this,
            1,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        manager.notify(
            UNLOCK_NOTIFICATION_ID,
            Notification.Builder(this, UNLOCK_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_steplock)
                .setSubText(getString(R.string.mascot_name))
                .setContentTitle(getString(R.string.unlock_notification_title))
                .setContentText(getString(R.string.unlock_notification_text))
                .setContentIntent(openApp)
                .setAutoCancel(true)
                .build(),
        )
    }

    private fun notifyEveningWalk(remainingSteps: Int) {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                NUDGE_CHANNEL_ID,
                getString(R.string.nudge_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
        val text = getString(R.string.nudge_notification_text)
        manager.notify(
            NUDGE_NOTIFICATION_ID,
            Notification.Builder(this, NUDGE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_steplock)
                .setSubText(getString(R.string.mascot_name))
                .setContentTitle(
                    getString(R.string.nudge_notification_title, "%,d".format(remainingSteps)),
                )
                .setContentText(text)
                .setStyle(Notification.BigTextStyle().bigText(text))
                .setContentIntent(
                    PendingIntent.getActivity(
                        this,
                        2,
                        Intent(this, MainActivity::class.java),
                        PendingIntent.FLAG_IMMUTABLE,
                    ),
                )
                .setAutoCancel(true)
                .build(),
        )
    }

    private fun startWatchNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.watch_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )

        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_steplock)
            .setSubText(getString(R.string.mascot_name))
            .setContentTitle(getString(R.string.watch_notification_title))
            .setContentText(getString(R.string.watch_notification_text))
            .setContentIntent(openApp)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        private const val CHANNEL_ID = "steplock_watch"
        private const val NOTIFICATION_ID = 21
        private const val UNLOCK_CHANNEL_ID = "steplock_unlocked"
        private const val UNLOCK_NOTIFICATION_ID = 22
        private const val NUDGE_CHANNEL_ID = "steplock_nudge"
        private const val NUDGE_NOTIFICATION_ID = 23

        /** 저녁 산책을 권하는 시간대. 너무 이르면 잔소리, 너무 늦으면 걸을 수 없습니다. */
        private val NUDGE_HOURS = 19..21
        private const val POLL_INTERVAL_MS = 1_000L
        private const val EVENT_WINDOW_MS = 10_000L
        private const val SLEEP_REFRESH_MS = 10 * 60_000L

        /**
         * 하루 기록을 덮어쓰는 간격. 자정 직전 기록이 최대 이만큼 낡을 수 있어서,
         * 마지막 1분 걸음은 그날 몫으로 남지 않을 수 있습니다 — 한 시간에 60번
         * 쓰지 않으면서 잃는 양을 가장 줄이는 지점으로 1분을 골랐습니다.
         */
        private const val RECORD_INTERVAL_MS = 60_000L

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, AppWatchService::class.java),
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AppWatchService::class.java))
        }
    }
}
