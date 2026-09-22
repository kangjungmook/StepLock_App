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
                if (!UnlockEvaluator.isUnlocked(current.settings, stat)) {
                    shownForAppId = blockedPackage
                    startActivity(LockActivity.intent(this, blockedPackage))
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
