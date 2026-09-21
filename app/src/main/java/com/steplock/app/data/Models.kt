package com.steplock.app.data

import java.time.LocalDate

/**
 * 로컬 레코드는 기기별 UUID로 저장하고, 로그인 후 서버 계정에 귀속(claim)시킬 수 있도록
 * accountId를 nullable로 함께 들고 갑니다. 저장(DataStore/Room) 연결은 아직 없습니다.
 */
data class LockSettings(
    val deviceUuid: String,
    val accountId: String? = null,
    val stepGoal: Int = 8000,
    val sleepGoalHours: Float = 7f,
    val pomodoroGoal: Int = 3,
    val stepsEnabled: Boolean = true,
    val sleepEnabled: Boolean = true,
    val pomodoroEnabled: Boolean = false,
    val requireAllConditions: Boolean = false,
    val blockedAppIds: Set<String> = emptySet(),
)

data class DailyStat(
    val deviceUuid: String,
    val accountId: String? = null,
    val date: LocalDate,
    val steps: Int,
    val sleepMinutes: Int,
    val pomodoroSessions: Int,
)

/** 차단 대상 앱. 실제 앱에서는 PackageManager에서 읽어 옵니다. */
data class BlockedApp(
    val id: String,
    val name: String,
    val subtitle: String,
    val initial: String,
)

sealed interface AuthState {
    data object Unknown : AuthState
    data object Guest : AuthState
    data class SignedIn(val accountId: String) : AuthState
}
