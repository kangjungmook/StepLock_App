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
import com.steplock.app.ui.PomodoroUiState
import com.steplock.app.ui.components.IconTapTarget
import com.steplock.app.ui.components.MascotMood
import com.steplock.app.ui.components.PrimaryButton
import com.steplock.app.ui.components.ProgressRing
import com.steplock.app.ui.components.SlIcons
import com.steplock.app.ui.components.StepLockMascot
import com.steplock.app.ui.components.TextLink
import com.steplock.app.ui.theme.SlColor
import com.steplock.app.ui.theme.SlDimen
import com.steplock.app.ui.theme.SlText
import com.steplock.app.ui.theme.StepLockTheme
import com.steplock.app.ui.util.formatCountdown

@Composable
fun PomodoroScreen(
    state: PomodoroUiState,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: () -> Unit,
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
            PrimaryButton(
                text = when {
                    state.running -> stringResource(R.string.pomodoro_pause)
                    state.paused -> stringResource(R.string.pomodoro_resume)
                    else -> stringResource(R.string.pomodoro_start, Pomodoro.SESSION_MINUTES)
                },
                onClick = when {
                    state.running -> onPause
                    state.paused -> onResume
                    else -> onStart
                },
            )
            if (state.running || state.paused) {
                TextLink(
                    text = stringResource(R.string.pomodoro_reset),
                    onClick = onReset,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.pomodoro_hint, Pomodoro.SESSION_MINUTES),
                    style = SlText.Caption,
                    color = SlColor.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
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
                paused = false,
                sessionsToday = 2,
                goal = 3,
            ),
            onBack = {},
            onStart = {},
            onPause = {},
            onResume = {},
            onReset = {},
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
                paused = false,
                sessionsToday = 0,
                goal = 3,
            ),
            onBack = {},
            onStart = {},
            onPause = {},
            onResume = {},
            onReset = {},
        )
    }
}
