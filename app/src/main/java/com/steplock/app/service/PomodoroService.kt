package com.steplock.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.steplock.app.MainActivity
import com.steplock.app.R
import com.steplock.app.data.Pomodoro
import com.steplock.app.data.SettingsRepository
import com.steplock.app.ui.util.formatCountdown
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 집중 세션이 화면을 벗어나도 이어지게 하고, 남은 시간을 알림에 보여줍니다.
 * 세션 상태는 DataStore가 들고 있어서 서비스가 죽어도 종료 시각으로 복구됩니다.
 */
class PomodoroService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var repository: SettingsRepository

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        repository = SettingsRepository(this)
        createChannel()
        startForegroundWith(
            getString(R.string.pomodoro_notification_running, formatCountdown(Pomodoro.SESSION_MS)),
        )
        scope.launch { trackSession() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun trackSession() {
        val preferences = repository.preferences.stateIn(scope)
        val serviceStartedAt = System.currentTimeMillis()

        while (currentCoroutineContext().isActive) {
            val state = preferences.value.pomodoro
            when {
                state.endsAt != null -> {
                    val remaining = state.endsAt - System.currentTimeMillis()
                    if (remaining <= 0) {
                        repository.completePomodoroSession()
                        notifySessionDone()
                        stopSelf()
                        return
                    }
                    updateNotification(
                        getString(R.string.pomodoro_notification_running, formatCountdown(remaining)),
                    )
                }

                state.pausedRemainingMs != null -> updateNotification(
                    getString(
                        R.string.pomodoro_notification_paused,
                        formatCountdown(state.pausedRemainingMs),
                    ),
                )

                // 화면에서 시작을 누른 직후에는 저장이 아직 안 끝났을 수 있습니다.
                System.currentTimeMillis() - serviceStartedAt > IDLE_GRACE_MS -> {
                    stopSelf()
                    return
                }
            }
            delay(TICK_MS)
        }
    }

    private fun createChannel() {
        notificationManager()?.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.pomodoro_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
    }

    private fun startForegroundWith(text: String) {
        val notification = buildNotification(text, ongoing = true)
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

    private fun updateNotification(text: String) {
        notificationManager()?.notify(NOTIFICATION_ID, buildNotification(text, ongoing = true))
    }

    private fun notifySessionDone() {
        notificationManager()?.notify(
            DONE_NOTIFICATION_ID,
            Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_steplock)
                .setContentTitle(getString(R.string.pomodoro_notification_done_title))
                .setContentText(getString(R.string.pomodoro_notification_done_text))
                .setContentIntent(openAppIntent())
                .setAutoCancel(true)
                .build(),
        )
    }

    private fun buildNotification(text: String, ongoing: Boolean): Notification =
        Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_steplock)
            .setContentTitle(getString(R.string.condition_pomodoro))
            .setContentText(text)
            .setContentIntent(openAppIntent())
            .setOngoing(ongoing)
            .setOnlyAlertOnce(true)
            .build()

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE,
    )

    private fun notificationManager(): NotificationManager? =
        getSystemService(NotificationManager::class.java)

    companion object {
        private const val CHANNEL_ID = "steplock_pomodoro"
        private const val NOTIFICATION_ID = 31
        private const val DONE_NOTIFICATION_ID = 32
        private const val TICK_MS = 500L
        private const val IDLE_GRACE_MS = 3_000L

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, PomodoroService::class.java),
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, PomodoroService::class.java))
        }
    }
}
