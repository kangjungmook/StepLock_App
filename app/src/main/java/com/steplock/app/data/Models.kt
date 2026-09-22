package com.steplock.app.data

import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * 조건을 **느슨하게** 바꿀 때 기다리는 기간.
 *
 * 엄하게 바꾸는 것(목표 올리기, 조건 켜기, 앱 추가)은 언제나 즉시 적용됩니다.
 * 느슨하게 바꾸는 것만 기다립니다 — 막고 싶은 건 "지금 보고 싶어서" 설정을 고치는
 * 행동이고, 스스로를 더 옥죄는 방향은 막을 이유가 없습니다.
 *
 * **이 기간을 줄이는 것도 "느슨하게"에 들어갑니다.** 그러지 않으면 기간을 0으로
 * 바꾼 다음 아무거나 풀 수 있어서 장치 전체가 무의미해집니다. 7일로 두었다가
 * 마음이 바뀌면, 0으로 돌아가는 데도 7일이 걸립니다.
 */
enum class RelaxDelay(val days: Int) {
    Immediate(0),
    NextDay(1),
    ThreeDays(3),
    SevenDays(7),
    ;

    companion object {
        /**
         * 기본값은 **즉시**입니다.
         *
         * 이건 스스로를 묶는 장치라 사용자가 직접 고르는 편이 맞습니다. 기본으로
         * 켜 두면 처음 설치해서 목표를 자기에게 맞게 낮추는 사람이 영문도 모르고
         * 하루를 기다리게 되고, 그 사람은 앱을 지웁니다.
         */
        val Default = Immediate

        /** 저장은 일수로 합니다 — 항목 순서가 바뀌어도 값이 어긋나지 않습니다. */
        fun fromDays(days: Int): RelaxDelay = entries.firstOrNull { it.days == days } ?: Default
    }
}

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
    /** 잠글 앱의 **패키지 이름**. 사용자가 기기에 깔린 앱에서 직접 고릅니다. */
    val blockedAppIds: Set<String> = emptySet(),
    val relaxDelay: RelaxDelay = RelaxDelay.Default,
)

/**
 * [other] 보다 느슨한 항목이 하나라도 있으면 true.
 *
 * 하나라도 느슨해지면 변경 전체를 대기로 돌립니다. 설정 화면은 버튼·스위치 하나가
 * 한 번의 변경이라 느슨함과 엄함이 섞인 변경은 사실상 생기지 않고, 섞였다면
 * 기다리는 쪽이 안전합니다.
 *
 * `blockedAppIds` 는 **빠진 앱이 있는지**로 봅니다 — 잠글 앱을 목록에서 빼는 건
 * 그 앱의 잠금을 푸는 것과 같습니다.
 */
fun LockSettings.isLooserThan(other: LockSettings): Boolean =
    stepGoal < other.stepGoal ||
        sleepGoalHours < other.sleepGoalHours ||
        pomodoroGoal < other.pomodoroGoal ||
        (!stepsEnabled && other.stepsEnabled) ||
        (!sleepEnabled && other.sleepEnabled) ||
        (!pomodoroEnabled && other.pomodoroEnabled) ||
        (!requireAllConditions && other.requireAllConditions) ||
        !blockedAppIds.containsAll(other.blockedAppIds) ||
        relaxDelay.days < other.relaxDelay.days

data class DailyStat(
    val deviceUuid: String,
    val accountId: String? = null,
    val date: LocalDate,
    val steps: Int,
    val sleepMinutes: Int,
    val pomodoroSessions: Int,
)

sealed interface AuthState {
    data object Unknown : AuthState
    data object Guest : AuthState
    data class SignedIn(val accountId: String) : AuthState
}

data class AppPreferences(
    /** **실제로 적용 중인** 설정. 잠금 판정과 홈 화면이 이걸 씁니다. */
    val settings: LockSettings,
    /**
     * 사용자가 설정 화면에서 정해 둔 값. 예약된 완화가 있으면 [settings] 와 다릅니다.
     * 설정 화면은 이걸 보여 줘야 방금 누른 게 반영돼 보입니다.
     */
    val desiredSettings: LockSettings,
    /** 예약된 완화가 적용되는 날. null 이면 예약이 없습니다. */
    val settingsApplyOn: LocalDate?,
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
