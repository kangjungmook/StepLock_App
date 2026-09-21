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
import com.steplock.app.data.BlockedApp
import com.steplock.app.data.BlockedAppCatalog
import com.steplock.app.data.DailyStat
import com.steplock.app.data.SettingsRepository
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
import java.util.concurrent.atomic.AtomicLong

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
        val preferences = repository.preferences.stateIn(scope)
        val steps = StepTracker(this, repository).todaySteps()
            .stateIn(scope, SharingStarted.Eagerly, 0)

        while (currentCoroutineContext().isActive) {
            val current = preferences.value
            val blockedApp = foregroundPackage(usageStats)
                ?.let { BlockedAppCatalog.byPackage(it) }
                ?.takeIf { it.id in current.settings.blockedAppIds }

            if (blockedApp == null) {
                shownForAppId = null
            } else if (shownForAppId != blockedApp.id && !isTemporarilyAllowed()) {
                val stat = DailyStat(
                    deviceUuid = current.settings.deviceUuid,
                    accountId = current.settings.accountId,
                    date = LocalDate.now(),
                    steps = steps.value,
                    sleepMinutes = 0,
                    pomodoroSessions = 0,
                )
                if (!UnlockEvaluator.isUnlocked(current.settings, stat)) {
                    shownForAppId = blockedApp.id
                    showLock(blockedApp)
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

    private fun showLock(app: BlockedApp) {
        startActivity(LockActivity.intent(this, app.id))
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

        private val temporaryAllowUntil = AtomicLong(0L)

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, AppWatchService::class.java),
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AppWatchService::class.java))
        }

        fun allowTemporarily(minutes: Int) {
            temporaryAllowUntil.set(System.currentTimeMillis() + minutes * 60_000L)
        }

        fun isTemporarilyAllowed(): Boolean =
            System.currentTimeMillis() < temporaryAllowUntil.get()
    }
}
