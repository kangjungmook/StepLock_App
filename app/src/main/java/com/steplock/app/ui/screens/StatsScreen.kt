package com.steplock.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.steplock.app.R
import com.steplock.app.data.DailyStat
import com.steplock.app.data.LockSettings
import com.steplock.app.data.SampleData
import com.steplock.app.ui.components.BottomNavBar
import com.steplock.app.ui.components.ConditionRow
import com.steplock.app.ui.components.IconTile
import com.steplock.app.ui.components.NavTab
import com.steplock.app.ui.components.SectionLabel
import com.steplock.app.ui.components.SlDivider
import com.steplock.app.ui.components.SlIcons
import com.steplock.app.ui.components.SlPanel
import com.steplock.app.ui.components.WeeklyBarChart
import com.steplock.app.ui.theme.SlColor
import com.steplock.app.ui.theme.SlDimen
import com.steplock.app.ui.theme.SlText
import com.steplock.app.ui.theme.StepLockTheme
import com.steplock.app.ui.util.formatThousands
import com.steplock.app.ui.util.sleepGoalMinutes
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun StatsScreen(
    weekly: List<DailyStat>,
    settings: LockSettings,
    selectedTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sleepGoalMinutes = sleepGoalMinutes(settings.sleepGoalHours)
    val averageSteps = if (weekly.isEmpty()) 0 else weekly.sumOf { it.steps } / weekly.size
    val weekdayFormatter = DateTimeFormatter.ofPattern(
        stringResource(R.string.stats_weekday_pattern),
        Locale.KOREAN,
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SlColor.Background),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = SlDimen.ScreenPadding,
                    end = SlDimen.ScreenPadding,
                    top = 12.dp,
                    bottom = 24.dp,
                ),
        ) {
            Text(
                text = stringResource(R.string.stats_title),
                style = SlText.Greeting,
                color = SlColor.TextPrimary,
            )

            Spacer(Modifier.height(24.dp))
            SectionLabel(stringResource(R.string.stats_section_steps))
            Spacer(Modifier.height(12.dp))
            SlPanel(contentPadding = PaddingValues(SlDimen.PanelPadding)) {
                Text(
                    text = stringResource(
                        R.string.stats_today_steps,
                        weekly.lastOrNull()?.steps?.formatThousands() ?: "0",
                    ),
                    style = SlText.RowTitle,
                    color = SlColor.TextPrimary,
                )
                Spacer(Modifier.height(16.dp))
                WeeklyBarChart(
                    values = weekly.map { it.steps },
                    goal = settings.stepGoal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                )
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    weekly.forEach { day ->
                        Text(
                            text = day.date.format(weekdayFormatter),
                            style = SlText.LabelSm,
                            color = SlColor.TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(
                        R.string.stats_steps_summary,
                        settings.stepGoal.formatThousands(),
                        averageSteps.formatThousands(),
                    ),
                    style = SlText.Caption,
                    color = SlColor.TextSecondary,
                )
            }

            Spacer(Modifier.height(28.dp))
            SectionLabel(stringResource(R.string.stats_section_conditions))
            Spacer(Modifier.height(12.dp))
            SlPanel {
                AchievementRow(
                    icon = SlIcons.Steps,
                    title = stringResource(R.string.condition_steps),
                    achievedByDay = weekly.map { it.steps >= settings.stepGoal },
                )
                SlDivider()
                AchievementRow(
                    icon = SlIcons.Moon,
                    title = stringResource(R.string.condition_sleep),
                    achievedByDay = weekly.map { it.sleepMinutes >= sleepGoalMinutes },
                )
                SlDivider()
                AchievementRow(
                    icon = SlIcons.Timer,
                    title = stringResource(R.string.condition_pomodoro),
                    achievedByDay = weekly.map { it.pomodoroSessions >= settings.pomodoroGoal },
                )
            }
        }

        BottomNavBar(selected = selectedTab, onSelect = onTabSelected)
    }
}

@Composable
private fun AchievementRow(icon: ImageVector, title: String, achievedByDay: List<Boolean>) {
    ConditionRow(
        title = title,
        value = stringResource(R.string.stats_achieved_days, achievedByDay.count { it }),
        leading = {
            IconTile(
                icon = icon,
                tint = SlColor.BrandDeep,
                background = SlColor.SurfaceAlt,
                size = SlDimen.TouchTarget,
                shape = RoundedCornerShape(SlDimen.RadiusField),
                iconSize = 22.dp,
            )
        },
        trailing = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                achievedByDay.forEach { achieved ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (achieved) SlColor.Brand else SlColor.Border),
                    )
                }
            }
        },
    )
}

@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun StatsScreenPreview() {
    StepLockTheme {
        StatsScreen(
            weekly = SampleData.weekly,
            settings = SampleData.settings,
            selectedTab = NavTab.Stats,
            onTabSelected = {},
        )
    }
}
