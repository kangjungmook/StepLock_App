package com.steplock.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.steplock.app.R
import com.steplock.app.data.Pomodoro
import com.steplock.app.ui.FocusPhase
import com.steplock.app.ui.PomodoroUiState
import com.steplock.app.ui.components.IconTapTarget
import com.steplock.app.ui.components.MascotMood
import com.steplock.app.ui.components.PrimaryButton
import com.steplock.app.ui.components.ProgressRing
import com.steplock.app.ui.components.SlIcons
import com.steplock.app.ui.components.StepLockMascot
import com.steplock.app.ui.components.StepiSays
import com.steplock.app.ui.theme.SlColor
import com.steplock.app.ui.theme.SlDimen
import com.steplock.app.ui.theme.SlText
import com.steplock.app.ui.theme.StepLockTheme
import com.steplock.app.ui.util.formatCountdown

@Composable
fun PomodoroScreen(
    state: PomodoroUiState,
    onBack: () -> Unit,
    /** 다음 세션을 바로 시작. [FocusPhase.Ready] 에서만 보입니다. */
    onStart: () -> Unit,
    /** 잠근 앱이 없을 때 고르러 가는 길. */
    onPickApps: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SlColor.Background)
            .safeDrawingPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTapTarget(
                icon = SlIcons.ArrowLeft,
                contentDescription = stringResource(R.string.action_back),
                onClick = onBack,
            )
            Text(
                text = stringResource(R.string.condition_pomodoro),
                style = SlText.ScreenTitle,
                color = SlColor.TextPrimary,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = SlDimen.ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // 세션이 도는 동안 앉아서 함께 기다립니다. 멈추거나 끝나면 일어섭니다 —
            // 자세 하나로 "지금 돌고 있는지"가 숫자를 읽지 않아도 보입니다.
            // 지금 상태에 맞는 한마디. 버튼 아래 안내 문구를 여기로 옮겨 와서,
            // 화면 아래쪽은 버튼만 남깁니다.
            StepiSays(
                text = when (state.phase) {
                    FocusPhase.Running -> stringResource(R.string.pomodoro_stepi_running)
                    FocusPhase.Ready -> stringResource(R.string.pomodoro_stepi_ready)
                    FocusPhase.GoalMet -> stringResource(R.string.pomodoro_stepi_goal_met)
                    FocusPhase.NoApps -> stringResource(
                        R.string.pomodoro_stepi_no_apps,
                        Pomodoro.SESSION_MINUTES,
                    )
                    FocusPhase.ConditionOff -> stringResource(R.string.pomodoro_stepi_off)
                },
            )
            Spacer(Modifier.height(16.dp))
            StepLockMascot(
                modifier = Modifier.size(width = 56.dp, height = 69.dp),
                mood = if (state.running) MascotMood.Focusing else MascotMood.Resting,
            )
            Spacer(Modifier.height(20.dp))

            ProgressRing(
                progress = state.progress,
                size = 200.dp,
                radius = 88.dp,
                strokeWidth = 14.dp,
                trackColor = SlColor.SurfaceAlt,
                progressColor = SlColor.Brand,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = formatCountdown(state.remainingMs),
                        style = SlText.RingValue,
                        color = SlColor.TextPrimary,
                    )
                    Text(
                        text = stringResource(R.string.pomodoro_ring_caption),
                        style = SlText.LabelSm,
                        color = SlColor.TextSecondary,
                    )
                }
            }

            Spacer(Modifier.height(28.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(state.goal) { index ->
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                if (index < state.sessionsToday) SlColor.Brand else SlColor.Border,
                            ),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(
                    R.string.pomodoro_sessions,
                    state.sessionsToday,
                    state.goal,
                ),
                style = SlText.CaptionMedium,
                color = SlColor.TextSecondary,
            )
        }

        Column(
            modifier = Modifier.padding(
                start = SlDimen.ScreenPadding,
                end = SlDimen.ScreenPadding,
                bottom = 24.dp,
            ),
        ) {
            // 세션이 도는 동안에는 버튼이 없습니다 — 멈춤도 처음부터도 없습니다.
            // 대신 왜 없는지를 버튼 자리에 적어 둡니다. 모든 상태에서 같은 높이를
            // 차지하게 해서 상태가 바뀔 때 위의 링이 튀지 않습니다.
            when (state.phase) {
                FocusPhase.Ready -> {
                    PrimaryButton(
                        text = stringResource(R.string.pomodoro_start_next, Pomodoro.SESSION_MINUTES),
                        onClick = onStart,
                    )
                    FocusNote(stringResource(R.string.pomodoro_start_note))
                }

                FocusPhase.NoApps -> {
                    PrimaryButton(
                        text = stringResource(R.string.home_pick_apps),
                        onClick = onPickApps,
                    )
                    FocusNote(stringResource(R.string.pomodoro_no_apps_note))
                }

                FocusPhase.Running -> FocusNote(
                    stringResource(R.string.pomodoro_running_note),
                    modifier = Modifier.height(SlDimen.CtaHeight + SlDimen.TouchTarget),
                )

                FocusPhase.GoalMet, FocusPhase.ConditionOff -> Spacer(
                    Modifier.height(SlDimen.CtaHeight + SlDimen.TouchTarget),
                )
            }
        }
    }
}

/** 버튼 아래(또는 버튼 자리)의 한 줄 안내. */
@Composable
private fun FocusNote(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = SlDimen.TouchTarget),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = SlText.Caption,
            color = SlColor.TextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun PomodoroScreenPreview() {
    StepLockTheme {
        PomodoroScreen(
            state = PomodoroUiState(
                remainingMs = 14 * 60_000L + 13_000L,
                progress = 0.43f,
                running = true,
                sessionsToday = 2,
                goal = 3,
                phase = FocusPhase.Running,
            ),
            onBack = {},
            onStart = {},
            onPickApps = {},
        )
    }
}

@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun PomodoroScreenIdlePreview() {
    StepLockTheme {
        PomodoroScreen(
            state = PomodoroUiState(
                remainingMs = Pomodoro.SESSION_MS,
                progress = 0f,
                running = false,
                sessionsToday = 1,
                goal = 3,
                phase = FocusPhase.Ready,
            ),
            onBack = {},
            onStart = {},
            onPickApps = {},
        )
    }
}
