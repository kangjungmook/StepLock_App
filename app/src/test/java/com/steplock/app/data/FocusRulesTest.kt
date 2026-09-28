package com.steplock.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 잠금이 걸려 있는지([isLocking])와 집중 세션을 저절로 시작할지([shouldAutoStartFocus]).
 *
 * 둘 다 "앱을 안 잠갔는데 조건이 진행된다", "타이머를 직접 눌러야 한다"는 문제를
 * 고치며 생긴 규칙이라, 어긋나면 바로 알 수 있게 여기서 확인합니다.
 */
class FocusRulesTest {

    private val locked = LockSettings(
        deviceUuid = "test",
        pomodoroEnabled = true,
        pomodoroGoal = 3,
        blockedAppIds = setOf("com.google.android.youtube"),
    )

    @Test
    fun `잠근 앱이 없으면 잠금이 걸리지 않는다`() {
        assertFalse(locked.copy(blockedAppIds = emptySet()).isLocking())
    }

    @Test
    fun `조건을 모두 끄면 잠금이 걸리지 않는다`() {
        val noConditions = locked.copy(
            stepsEnabled = false,
            sleepEnabled = false,
            pomodoroEnabled = false,
        )
        assertFalse(noConditions.isLocking())
    }

    @Test
    fun `앱을 잠그고 조건을 켜면 잠금이 걸린다`() {
        assertTrue(locked.isLocking())
    }

    @Test
    fun `앱을 잠그는 순간 집중 세션이 시작된다`() {
        assertTrue(shouldAutoStartFocus(locked, PomodoroState(sessionsToday = 0)))
    }

    @Test
    fun `잠근 앱이 없으면 집중 세션을 시작하지 않는다`() {
        assertFalse(
            shouldAutoStartFocus(locked.copy(blockedAppIds = emptySet()), PomodoroState()),
        )
    }

    @Test
    fun `집중 조건을 꺼 두면 시작하지 않는다`() {
        assertFalse(shouldAutoStartFocus(locked.copy(pomodoroEnabled = false), PomodoroState()))
    }

    @Test
    fun `오늘 목표를 채웠으면 시작하지 않는다`() {
        assertFalse(shouldAutoStartFocus(locked, PomodoroState(sessionsToday = 3)))
    }

    @Test
    fun `이미 도는 세션은 새로 덮지 않는다`() {
        val running = PomodoroState(endsAt = 1_000_000L + Pomodoro.SESSION_MS, sessionsToday = 1)
        assertFalse(shouldAutoStartFocus(locked, running))
    }

    @Test
    fun `세션 시작 시각은 끝나는 시각에서 세션 길이를 뺀 값이다`() {
        val running = PomodoroState(endsAt = 1_000_000L + Pomodoro.SESSION_MS)
        assertEquals(1_000_000L, running.startedAt)
        assertEquals(null, PomodoroState().startedAt)
    }
}
