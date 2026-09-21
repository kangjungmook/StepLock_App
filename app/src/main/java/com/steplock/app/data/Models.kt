package com.steplock.app.data

import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * 로컬 레코드는 기기별 UUID로 저장하고, 로그인 후 서버 계정에 귀속(claim)시킬 수 있도록
 * accountId를 nullable로 함께 들고 갑니다.
 */
data class LockSettings(
    val deviceUuid: String,
    val accountId: String? = null,
    /** 로그인 전에는 비어 있고, 계정이 붙으면 채워집니다. */
    val displayName: String? = null,
    val stepGoal: Int = 8000,
    val sleepGoalHours: Float = 7f,
    val pomodoroGoal: Int = 3,
    val stepsEnabled: Boolean = true,
    /** 수면·집중 타이머는 아직 데이터 소스가 없어 기본값을 꺼 둡니다. */
    val sleepEnabled: Boolean = false,
    val pomodoroEnabled: Boolean = false,
    val requireAllConditions: Boolean = false,
    val blockedAppIds: Set<String> = setOf("shorts", "reels", "tiktok"),
)

data class DailyStat(
    val deviceUuid: String,
    val accountId: String? = null,
    val date: LocalDate,
    val steps: Int,
    val sleepMinutes: Int,
    val pomodoroSessions: Int,
)

data class BlockedApp(
    val id: String,
    val name: String,
    val subtitle: String,
    val initial: String,
    val packageName: String,
)

sealed interface AuthState {
    data object Unknown : AuthState
    data object Guest : AuthState
    data class SignedIn(val accountId: String) : AuthState
}

data class AppPreferences(
    val settings: LockSettings,
    val onboardingCompleted: Boolean,
    val authState: AuthState,
    val pomodoro: PomodoroState,
    /** Health Connect에서 마지막으로 읽어 둔 오늘의 수면 분. */
    val sleepMinutesToday: Int,
)

/**
 * 진행 중인 세션은 종료 시각으로, 멈춘 세션은 남은 시간으로 저장합니다.
 * 앱이 죽어도 종료 시각만 있으면 남은 시간을 다시 계산할 수 있습니다.
 */
data class PomodoroState(
    val endsAt: Long? = null,
    val pausedRemainingMs: Long? = null,
    val sessionsToday: Int = 0,
) {
    val isRunning: Boolean get() = endsAt != null
    val isPaused: Boolean get() = endsAt == null && pausedRemainingMs != null
}

object Pomodoro {
    const val SESSION_MINUTES = 25
    const val SESSION_MS = SESSION_MINUTES * 60_000L
}

/**
 * 차단 대상 후보. 쇼츠·릴스는 각각 YouTube·Instagram 앱 안에 있어 앱 단위로 잠깁니다.
 */
object BlockedAppCatalog {
    val apps = listOf(
        BlockedApp("shorts", "쇼츠", "YouTube · 짧은 영상", "S", "com.google.android.youtube"),
        BlockedApp("reels", "릴스", "Instagram · 짧은 영상", "R", "com.instagram.android"),
        BlockedApp("tiktok", "틱톡", "TikTok · 짧은 영상", "T", "com.zhiliaoapp.musically"),
        BlockedApp("x", "엑스", "X · 짧은 영상", "X", "com.twitter.android"),
    )

    fun byId(id: String): BlockedApp? = apps.firstOrNull { it.id == id }

    fun byPackage(packageName: String): BlockedApp? = apps.firstOrNull { it.packageName == packageName }
}

/** 켜 둔 조건만 계산합니다. 전부 만족 모드가 꺼져 있으면 하나만 채워도 해제됩니다. */
object UnlockEvaluator {
    fun isUnlocked(settings: LockSettings, stat: DailyStat): Boolean {
        val results = buildList {
            if (settings.stepsEnabled) add(stat.steps >= settings.stepGoal)
            if (settings.sleepEnabled) add(stat.sleepMinutes >= (settings.sleepGoalHours * 60).roundToInt())
            if (settings.pomodoroEnabled) add(stat.pomodoroSessions >= settings.pomodoroGoal)
        }
        if (results.isEmpty()) return true
        return if (settings.requireAllConditions) results.all { it } else results.any { it }
    }
}
