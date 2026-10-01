package com.steplock.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.steplock.app.R
import com.steplock.app.data.Pomodoro
import com.steplock.app.ui.FocusPhase
import com.steplock.app.ui.PomodoroUiState
import com.steplock.app.ui.components.BubbleTail
import com.steplock.app.ui.components.IconTapTarget
import com.steplock.app.ui.components.MascotMood
import com.steplock.app.ui.components.MinuteGrid
import com.steplock.app.ui.components.PrimaryButton
import com.steplock.app.ui.components.SlIcons
import com.steplock.app.ui.components.StepLockMascot
import com.steplock.app.ui.components.StepiSays
import com.steplock.app.ui.theme.SlColor
import com.steplock.app.ui.theme.SlDimen
import com.steplock.app.ui.theme.SlText
import com.steplock.app.ui.theme.StepLockTheme
import com.steplock.app.ui.util.formatCountdown

/**
 * 집중 타이머 — 링 대신 **큰 남은 시간 + 25칸 + 오늘의 번호판**.
 *
 * 위에서 아래로 "지금 몇 분 남았나 → 이번 세션은 어디쯤인가 → 오늘은 몇 번째인가"를
 * 한 번씩만 말합니다. 남은 시간은 홈의 큰 숫자와 같은 글꼴로 왼쪽 정렬해서,
 * 카드 없이 여백만으로 덩어리를 나눕니다.
 */
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SlDimen.ScreenPadding),
        ) {
            Spacer(Modifier.height(16.dp))

            // 1. 남은 시간 — 화면에서 가장 큰 것.
            Text(
                text = formatCountdown(state.remainingMs),
                style = SlText.HomeNumeral.copy(fontSize = 120.sp, lineHeight = 120.sp),
                color = if (state.running) SlColor.TextPrimary else SlColor.TextTertiary,
                maxLines = 1,
                softWrap = false,
            )
            Text(
                text = stringResource(
                    if (state.running) R.string.pomodoro_left else R.string.pomodoro_waiting,
                ),
                style = SlText.HomeLine,
                color = SlColor.TextPrimary,
            )

            // 2. 이번 세션 — 25칸, 한 칸이 1분.
            Spacer(Modifier.height(32.dp))
            MinuteGrid(
                remainingMs = state.remainingMs,
                description = stringResource(
                    R.string.pomodoro_grid_desc,
                    formatCountdown(state.remainingMs),
                ),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.pomodoro_grid_caption),
                style = SlText.Caption,
                color = SlColor.TextTertiary,
            )

            // 3. 오늘 — 번호판.
            Spacer(Modifier.height(40.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = stringResource(R.string.pomodoro_today_title),
                    style = SlText.SectionTitle,
                    color = SlColor.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.home_pomodoro_value, state.sessionsToday, state.goal),
                    style = SlText.CaptionMedium,
                    color = SlColor.TextSecondary,
                )
            }
            Spacer(Modifier.height(12.dp))
            SessionPlates(
                done = state.sessionsToday,
                goal = state.goal,
                running = state.running,
            )

            // 4. 스텝이의 한마디 — 지금 상태에서 할 수 있는 것.
            Spacer(Modifier.height(32.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StepLockMascot(
                    modifier = Modifier.size(width = 40.dp, height = 49.dp),
                    mood = if (state.running) MascotMood.Focusing else MascotMood.Resting,
                )
                Spacer(Modifier.width(8.dp))
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
                    tail = BubbleTail.Start,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(24.dp))
        }

        Column(
            modifier = Modifier.padding(
                start = SlDimen.ScreenPadding,
                end = SlDimen.ScreenPadding,
                bottom = 24.dp,
            ),
        ) {
            // 세션이 도는 동안에는 버튼이 없습니다 — 멈춤도 처음부터도 없습니다.
            // 대신 왜 없는지를 버튼 자리에 적어 둡니다.
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

                FocusPhase.Running -> FocusNote(stringResource(R.string.pomodoro_running_note))

                FocusPhase.GoalMet, FocusPhase.ConditionOff -> Unit
            }
        }
    }
}

/**
 * 오늘의 세션 번호판. 끝난 판은 흑연색으로 채우고, 지금 도는 판은 주황 테두리,
 * 남은 판은 흐린 테두리입니다. 목표를 넘겨 더 한 날은 판이 늘어납니다.
 */
@Composable
private fun SessionPlates(done: Int, goal: Int, running: Boolean) {
    val count = maxOf(goal, done + if (running) 1 else 0).coerceIn(1, MAX_PLATES)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (i in 0 until count) {
            val number = i + 1
            val isDone = i < done
            val isNow = running && i == done
            val shape = RoundedCornerShape(SlDimen.RadiusSmall)
            val description = when {
                isDone -> stringResource(R.string.pomodoro_plate_done, number)
                isNow -> stringResource(R.string.pomodoro_plate_now, number)
                else -> stringResource(R.string.pomodoro_plate_todo, number)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(shape)
                    .then(
                        when {
                            isDone -> Modifier.background(SlColor.TextPrimary)
                            isNow -> Modifier.border(2.dp, SlColor.Brand, shape)
                            else -> Modifier.border(1.dp, SlColor.Border, shape)
                        },
                    )
                    .semantics { contentDescription = description },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = number.toString(),
                    style = SlText.RowNumeral,
                    color = when {
                        isDone -> SlColor.Background
                        isNow -> SlColor.BrandInk
                        else -> SlColor.TextTertiary
                    },
                )
            }
        }
    }
}

private const val MAX_PLATES = 8

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
