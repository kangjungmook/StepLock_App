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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.steplock.app.ui.components.IconTapTarget
import com.steplock.app.ui.components.IconTile
import com.steplock.app.ui.components.PrimaryButton
import com.steplock.app.ui.components.ProgressRing
import com.steplock.app.ui.components.SlIcons
import com.steplock.app.ui.components.StatusChip
import com.steplock.app.ui.components.TextLink
import com.steplock.app.ui.theme.SlColor
import com.steplock.app.ui.theme.SlText
import com.steplock.app.ui.util.formatThousands
import com.steplock.app.ui.util.sleepGoalLabel
import com.steplock.app.ui.util.sleepGoalMinutes
import com.steplock.app.ui.util.withTopicParticle
import kotlin.math.roundToInt

/** 차단된 앱을 열면 위에 덮이는 전체 화면. 다크 팔레트를 쓰는 유일한 화면입니다. */
@Composable
fun LockOverlayScreen(
    appName: String,
    stat: DailyStat,
    settings: LockSettings,
    onDismiss: () -> Unit,
    onTemporaryAllow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stepProgress = (stat.steps.toFloat() / settings.stepGoal).coerceIn(0f, 1f)
    val percentLabel = "${(stepProgress * 100).roundToInt()}%"
    val sleepAchieved = stat.sleepMinutes >= sleepGoalMinutes(settings.sleepGoalHours)
    val pomodoroAchieved = stat.pomodoroSessions >= settings.pomodoroGoal

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SlColor.Dark.Background)
            .safeDrawingPadding()
            .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            IconTapTarget(
                icon = SlIcons.Close,
                contentDescription = stringResource(R.string.action_close),
                onClick = onDismiss,
                tint = SlColor.Dark.TextIcon,
                iconSize = 18.dp,
                background = SlColor.Dark.Surface,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            IconTile(
                icon = SlIcons.LockBadge,
                tint = SlColor.Dark.AmberIcon,
                background = SlColor.Dark.AmberTint,
                size = 80.dp,
                shape = RoundedCornerShape(28.dp),
                iconSize = 34.dp,
            )

            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.lock_title, withTopicParticle(appName)),
                style = SlText.LockTitle,
                color = SlColor.Dark.TextPrimary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.lock_desc),
                style = SlText.Body,
                color = SlColor.Dark.TextMuted,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(28.dp))
            ProgressRing(
                progress = stepProgress,
                size = 200.dp,
                radius = 88.dp,
                strokeWidth = 14.dp,
                trackColor = SlColor.Dark.SurfaceAlt,
                progressColor = SlColor.Amber,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = percentLabel,
                        style = SlText.RingValue,
                        color = SlColor.Dark.TextPrimary,
                    )
                    Text(
                        text = stringResource(R.string.lock_ring_caption),
                        style = SlText.LabelSm,
                        color = SlColor.Dark.TextMuted,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(
                    R.string.lock_remaining,
                    (settings.stepGoal - stat.steps).coerceAtLeast(0).formatThousands(),
                ),
                style = SlText.Remaining,
                color = SlColor.Dark.AmberText,
            )

            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusChip(
                    icon = SlIcons.StepsCompact,
                    text = percentLabel,
                    state = if (stepProgress >= 1f) ChipState.Achieved else ChipState.Active,
                )
                if (sleepAchieved) {
                    StatusChip(
                        icon = SlIcons.CheckThin,
                        text = stringResource(R.string.lock_chip_sleep_done),
                        state = ChipState.Achieved,
                    )
                } else {
                    StatusChip(
                        icon = SlIcons.Moon,
                        text = sleepGoalLabel(settings.sleepGoalHours),
                        state = ChipState.Idle,
                    )
                }
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
