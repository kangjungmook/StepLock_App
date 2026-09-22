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
    val accountEmail: String? = null,
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
    val temporaryAllow: TemporaryAllowState,
    /** Health Connect에서 마지막으로 읽어 둔 오늘의 수면 분. */
    val sleepMinutesToday: Int,
    /** 통계용 일별 기록. 오래된 날부터 정렬됩니다. */
    val history: List<DailyStat>,
    /** 설정을 마지막으로 바꾼 시각. 서버와 비교해 최신 쪽을 택합니다. */
    val settingsUpdatedAt: Long,
)

/**
 * 진행 중인 세션은 종료 시각으로, 멈춘 세션은 남은 시간으로 저장합니다.
 * 앱이 죽어도 종료 시각만 있으면 남은 시간을 다시 계산할 수 있습니다.
 */
/**
 * "5분만 임시로 허용하기"의 상태.
 *
 * 하루 한도를 두지 않으면 잠금 화면이 뜰 때마다 눌러서 앱을 무력화할 수 있습니다.
 * 종료 시각과 사용 횟수를 DataStore에 두기 때문에 앱이나 서비스를 강제 종료해도
 * 허용이 풀리거나 횟수가 되돌아가지 않습니다.
 */
data class TemporaryAllowState(
    val allowedUntil: Long? = null,
    val usedToday: Int = 0,
    /** 광고를 끝까지 봐서 오늘 추가로 얻은 횟수. [TemporaryAllow.AD_BONUS_LIMIT] 까지. */
    val bonusEarnedToday: Int = 0,
) {
    fun isActive(now: Long = System.currentTimeMillis()): Boolean =
        allowedUntil != null && now < allowedUntil

    /** 오늘 더 쓸 수 있는 횟수 — 기본 한도에 광고로 얻은 보너스를 더한 값입니다. */
    val remainingToday: Int
        get() = (TemporaryAllow.DAILY_LIMIT + bonusEarnedToday - usedToday).coerceAtLeast(0)

    /** 광고로 더 받을 수 있는 횟수. 0이면 광고 버튼도 사라집니다. */
    val bonusRemaining: Int
        get() = (TemporaryAllow.AD_BONUS_LIMIT - bonusEarnedToday).coerceAtLeast(0)
}

object TemporaryAllow {
    const val DAILY_LIMIT = 3

    /**
     * 광고로 늘릴 수 있는 한도. 기본 3회 + 보너스 2회 = **하루 최대 5회**입니다.
     *
     * 상한이 없으면 광고를 계속 보면서 잠금을 무력화할 수 있고, 그러면 이 앱이
     * 하려던 일이 사라집니다. 광고 수익보다 앱의 목적이 먼저라서 여기서 끊습니다.
     */
    const val AD_BONUS_LIMIT = 2

    const val MINUTES = 5
}

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
