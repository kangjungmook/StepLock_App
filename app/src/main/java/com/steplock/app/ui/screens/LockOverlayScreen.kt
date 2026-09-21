package com.steplock.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.steplock.app.R
import com.steplock.app.data.DailyStat
import com.steplock.app.data.LockSettings
import com.steplock.app.data.SampleData
import com.steplock.app.ui.components.ChipState
import com.steplock.app.ui.components.MascotMood
import com.steplock.app.ui.components.PrimaryButton
import com.steplock.app.ui.components.ProgressRing
import com.steplock.app.ui.components.SlIcons
import com.steplock.app.ui.components.StatusChip
import com.steplock.app.ui.components.StepLockMascot
import com.steplock.app.ui.components.TextLink
import com.steplock.app.ui.theme.SlColor
import com.steplock.app.ui.theme.SlText
import com.steplock.app.ui.util.UnlockCondition
import com.steplock.app.ui.util.enabledConditions
import com.steplock.app.ui.util.isAchieved
import com.steplock.app.ui.util.primaryCondition
import com.steplock.app.ui.util.progress
import com.steplock.app.ui.util.remainingText
import com.steplock.app.ui.util.sleepGoalLabel
import com.steplock.app.ui.util.withTopicParticle
import kotlin.math.roundToInt

/**
 * 차단된 앱을 열면 위에 덮이는 전체 화면. 다크 팔레트를 쓰는 유일한 화면입니다.
 *
 * 한 화면에 조건을 모두 늘어놓으면 무엇을 해야 할지 오히려 흐려지므로,
 * 켜 둔 조건 중 하나만 링으로 크게 보여 주고 나머지는 칩으로 요약합니다.
 */
@Composable
fun LockOverlayScreen(
    appName: String,
    stat: DailyStat,
    settings: LockSettings,
    onDismiss: () -> Unit,
    onTemporaryAllow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sleepAchieved = UnlockCondition.Sleep.isAchieved(stat, settings)
    val pomodoroAchieved = UnlockCondition.Pomodoro.isAchieved(stat, settings)

    // 켜 둔 조건만 다룹니다. 끈 조건을 보여 주면 열 수 없는 자물쇠처럼 읽힙니다.
    // 홈과 같은 기준·같은 문구를 쓰려고 조건 계산은 공용 유틸에 있습니다.
    val hero = primaryCondition(settings)
    val heroProgress = hero.progress(stat, settings)
    val enabledCount = enabledConditions(settings).size

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SlColor.Dark.Background)
            .safeDrawingPadding()
            .padding(horizontal = 24.dp, vertical = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // 목표를 채우면 캐릭터도 걸음을 멈춥니다. 모든 조건을 요구할 때만 볼 수 있는 상태입니다.
            StepLockMascot(
                modifier = Modifier.size(width = 80.dp, height = 99.dp),
                mood = if (heroProgress >= 1f) MascotMood.Resting else MascotMood.Walking,
                bodyColor = SlColor.Dark.GreenIcon,
                shadeColor = SlColor.Dark.GreenDeep,
                eyeColor = SlColor.Dark.GreenTint,
                footprintColor = SlColor.Dark.Border,
            )

            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.lock_title, withTopicParticle(appName)),
                style = SlText.LockTitle,
                color = SlColor.Dark.TextPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                // 채운 조건이 히어로일 수 있어서(전부 만족 모드) 공용 확장이 분기합니다.
                text = hero.remainingText(stat, settings),
                style = SlText.Remaining,
                color = SlColor.Dark.AmberText,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(32.dp))
            ProgressRing(
                progress = heroProgress,
                size = 176.dp,
                radius = 76.dp,
                strokeWidth = 12.dp,
                trackColor = SlColor.Dark.SurfaceAlt,
                progressColor = SlColor.Dark.AmberRing,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "${(heroProgress * 100).roundToInt()}%",
                        style = SlText.RingValue,
                        color = SlColor.Dark.TextPrimary,
                    )
                    Text(
                        text = stringResource(hero.goalCaptionRes),
                        style = SlText.LabelSm,
                        color = SlColor.Dark.TextMuted,
                    )
                }
            }

            // 링으로 보여 준 조건은 빼고, 남은 조건만 칩으로 요약합니다.
            val showSleepChip = settings.sleepEnabled && hero != UnlockCondition.Sleep
            val showPomodoroChip = settings.pomodoroEnabled && hero != UnlockCondition.Pomodoro
            if (showSleepChip || showPomodoroChip) {
                Spacer(Modifier.height(28.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (showSleepChip) {
                        StatusChip(
                            icon = if (sleepAchieved) SlIcons.CheckThin else SlIcons.Moon,
                            text = if (sleepAchieved) {
                                stringResource(R.string.lock_chip_sleep_done)
                            } else {
                                stringResource(
                                    R.string.lock_chip_sleep_goal,
                                    sleepGoalLabel(settings.sleepGoalHours),
                                )
                            },
                            state = if (sleepAchieved) ChipState.Achieved else ChipState.Idle,
                        )
                    }
                    if (showPomodoroChip) {
                        StatusChip(
                            icon = SlIcons.TimerCompact,
                            text = stringResource(
                                R.string.lock_chip_sessions,
                                stat.pomodoroSessions,
                                settings.pomodoroGoal,
                            ),
                            state = if (pomodoroAchieved) ChipState.Achieved else ChipState.Idle,
                        )
                    }
                }
            }

            if (enabledCount > 1) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(
                        if (settings.requireAllConditions) {
                            R.string.lock_rule_all
                        } else {
                            R.string.lock_rule_any
                        },
                    ),
                    style = SlText.LabelSm,
                    color = SlColor.Dark.TextMuted,
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        PrimaryButton(
            text = stringResource(R.string.lock_dismiss),
            onClick = onDismiss,
            containerColor = SlColor.Dark.SurfaceAlt,
            contentColor = SlColor.Dark.TextBright,
        )
        TextLink(
            text = stringResource(R.string.lock_temporary_allow),
            onClick = onTemporaryAllow,
            modifier = Modifier.fillMaxWidth(),
            style = SlText.LinkSm,
            color = SlColor.Dark.TextLink,
        )
    }
}

@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun LockOverlayScreenPreview() {
    LockOverlayScreen(
        appName = "쇼츠",
        stat = SampleData.today,
        settings = SampleData.settings,
        onDismiss = {},
        onTemporaryAllow = {},
    )
}
