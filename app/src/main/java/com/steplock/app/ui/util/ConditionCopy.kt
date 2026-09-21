package com.steplock.app.ui.util

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.steplock.app.R
import com.steplock.app.data.DailyStat
import com.steplock.app.data.LockSettings

/**
 * 잠금 해제 조건 세 가지.
 *
 * 홈과 잠금 화면이 같은 순서·같은 진행률·같은 문구를 써야 해서 여기로 모았습니다.
 * 두 화면이 따로 계산하면 "홈에서는 66%인데 잠금 화면에서는 다른 말을 하는" 상황이 생깁니다.
 */
enum class UnlockCondition(
    @StringRes val titleRes: Int,
    @StringRes val goalCaptionRes: Int,
) {
    Steps(R.string.condition_steps, R.string.lock_ring_caption),
    Sleep(R.string.condition_sleep, R.string.lock_ring_caption_sleep),
    Pomodoro(R.string.condition_pomodoro, R.string.lock_ring_caption_pomodoro),
}

/** 켜 둔 조건만. 끈 조건을 보여 주면 열 수 없는 자물쇠처럼 읽힙니다. */
fun enabledConditions(settings: LockSettings): List<UnlockCondition> = buildList {
    if (settings.stepsEnabled) add(UnlockCondition.Steps)
    if (settings.sleepEnabled) add(UnlockCondition.Sleep)
    if (settings.pomodoroEnabled) add(UnlockCondition.Pomodoro)
}

/** 크게 보여 줄 조건 — 켜 둔 것 중 먼저 오는 하나. */
fun primaryCondition(settings: LockSettings): UnlockCondition =
    enabledConditions(settings).firstOrNull() ?: UnlockCondition.Steps

fun UnlockCondition.progress(stat: DailyStat, settings: LockSettings): Float = when (this) {
    UnlockCondition.Steps -> ratio(stat.steps, settings.stepGoal)
    UnlockCondition.Sleep -> ratio(stat.sleepMinutes, sleepGoalMinutes(settings.sleepGoalHours))
    UnlockCondition.Pomodoro -> ratio(stat.pomodoroSessions, settings.pomodoroGoal)
}

fun UnlockCondition.isAchieved(stat: DailyStat, settings: LockSettings): Boolean =
    progress(stat, settings) >= 1f

/** 무엇을 더 하면 열리는지 한 줄. 이미 채운 조건이면 남은 조건을 가리킵니다. */
@Composable
fun UnlockCondition.remainingText(stat: DailyStat, settings: LockSettings): String = when {
    isAchieved(stat, settings) -> stringResource(R.string.lock_remaining_done)

    this == UnlockCondition.Steps -> stringResource(
        R.string.lock_remaining,
        (settings.stepGoal - stat.steps).coerceAtLeast(0).formatThousands(),
    )

    // 지금 당장 채울 수 없는 조건이라 남은 시간을 숫자로 재촉하지 않습니다.
    this == UnlockCondition.Sleep -> stringResource(R.string.lock_remaining_sleep)

    else -> stringResource(
        R.string.lock_remaining_pomodoro,
        (settings.pomodoroGoal - stat.pomodoroSessions).coerceAtLeast(1),
    )
}

/** 목표가 0이면 이미 채운 것으로 봅니다 — 0으로 나눠 NaN이 링에 흘러가지 않게. */
private fun ratio(value: Int, goal: Int): Float =
    if (goal <= 0) 1f else (value.toFloat() / goal).coerceIn(0f, 1f)
