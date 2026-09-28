package com.steplock.app.system

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import com.steplock.app.data.SettingsRepository
import kotlinx.coroutines.flow.first
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 집중 세션 도중 사용자가 시스템 설정에서 앱을 **강제 종료**했으면 그 세션을 지웁니다.
 *
 * 세션은 한 번 시작하면 앱 안에서는 멈출 수 없습니다. 대신 강제 종료와 앱 삭제는
 * 사용자가 끝낼 수 있는 마지막 길로 남겨 둡니다. 앱 삭제는 저장소가 통째로 사라지니
 * 따로 할 일이 없지만, 강제 종료는 저장소(DataStore)를 그대로 두기 때문에 다음에
 * 앱을 열었을 때 여기서 알아채고 지웁니다.
 *
 * 알아채는 방법은 안드로이드 11(API 30)부터 있는 **프로세스 종료 기록**입니다.
 * 이 앱의 마지막 종료 사유가 "사용자 요청"이고 그 시각이 세션을 시작한 뒤라면,
 * 세션 도중 강제 종료한 것입니다. 안드로이드 10 이하에는 이 기록이 없어서 세션이
 * 원래 끝나는 시각까지 이어집니다.
 *
 * 프로세스마다 **처음 한 번만** 봅니다. 종료 기록은 이전 프로세스에 관한 것이라,
 * 같은 프로세스에서 새로 시작한 세션을 다시 검사하면 안 됩니다.
 */
object ForceStopCheck {

    private val checked = AtomicBoolean(false)

    suspend fun discardFocusIfForceStopped(context: Context) {
        if (!checked.compareAndSet(false, true)) return
        val stoppedAt = lastForceStopAt(context) ?: return
        val repository = SettingsRepository(context.applicationContext)
        val startedAt = repository.preferences.first().pomodoro.startedAt ?: return
        if (stoppedAt >= startedAt) repository.discardPomodoroSession()
    }

    /** 이 앱의 바로 전 프로세스가 사용자 요청(강제 종료)으로 끝났으면 그 시각. */
    private fun lastForceStopAt(context: Context): Long? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        val manager = context.getSystemService(ActivityManager::class.java) ?: return null
        val last = runCatching {
            manager.getHistoricalProcessExitReasons(context.packageName, 0, 1)
        }.getOrNull()?.firstOrNull() ?: return null
        return last.timestamp.takeIf { last.reason == ApplicationExitInfo.REASON_USER_REQUESTED }
    }
}
