package com.steplock.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.steplock.app.R
import com.steplock.app.data.BlockedApp
import com.steplock.app.data.DailyStat
import com.steplock.app.data.LockSettings
import com.steplock.app.data.SampleData
import com.steplock.app.ui.components.AppListItem
import com.steplock.app.ui.components.BottomNavBar
import com.steplock.app.ui.components.ConditionRow
import com.steplock.app.ui.components.IconTile
import com.steplock.app.ui.components.LockBanner
import com.steplock.app.ui.components.NavTab
import com.steplock.app.ui.components.ProgressRing
import com.steplock.app.ui.components.SectionLabel
import com.steplock.app.ui.components.SlDivider
import com.steplock.app.ui.components.SlIcons
import com.steplock.app.ui.components.SlPanel
import com.steplock.app.ui.components.appBadgeColor
import com.steplock.app.ui.theme.SlColor
import com.steplock.app.ui.theme.SlDimen
import com.steplock.app.ui.theme.SlText
import com.steplock.app.ui.theme.StepLockTheme
import com.steplock.app.ui.util.durationLabel
import com.steplock.app.ui.util.formatThousands
import com.steplock.app.ui.util.sleepGoalLabel
import com.steplock.app.ui.util.sleepGoalMinutes
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    userName: String,
    stat: DailyStat,
    settings: LockSettings,
    apps: List<BlockedApp>,
    selectedTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    onManageLocks: () -> Unit,
    onAppClick: (BlockedApp) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lockedApps = apps.filter { it.id in settings.blockedAppIds }
    val stepProgress = (stat.steps.toFloat() / settings.stepGoal).coerceIn(0f, 1f)
    val sleepAchieved = stat.sleepMinutes >= sleepGoalMinutes(settings.sleepGoalHours)

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
                text = stringResource(R.string.home_greeting, userName),
                style = SlText.Greeting,
                color = SlColor.TextPrimary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stat.date.format(
                    DateTimeFormatter.ofPattern(
                        stringResource(R.string.home_date_pattern),
                        Locale.KOREAN,
                    ),
                ),
                style = SlText.RowValue,
                color = SlColor.TextSecondary,
            )

            Spacer(Modifier.height(20.dp))
            LockBanner(
                title = stringResource(R.string.home_locked_count, lockedApps.size),
                subtitle = lockedApps.joinToString(" · ") { it.name },
                actionLabel = stringResource(R.string.home_locked_manage),
                onClick = onManageLocks,
            )

            Spacer(Modifier.height(28.dp))
            SectionLabel(stringResource(R.string.home_section_conditions))
            Spacer(Modifier.height(12.dp))
            SlPanel {
                ConditionRow(
                    title = stringResource(R.string.condition_steps),
                    value = stringResource(
                        R.string.home_steps_value,
                        stat.steps.formatThousands(),
                        settings.stepGoal.formatThousands(),
                    ),
                    leading = {
                        ProgressRing(
                            progress = stepProgress,
                            size = SlDimen.TouchTarget,
                            radius = 18.dp,
                            strokeWidth = 4.dp,
                        ) {
                            Text(
                                text = "${(stepProgress * 100).roundToInt()}%",
                                style = SlText.RingPercent,
                                color = SlColor.BrandDeep,
                            )
                        }
                    },
                )
                SlDivider()
                ConditionRow(
                    title = stringResource(R.string.condition_sleep),
                    value = stringResource(
                        R.string.home_sleep_value,
                        durationLabel(stat.sleepMinutes),
                        sleepGoalLabel(settings.sleepGoalHours),
                    ),
                    leading = {
                        if (sleepAchieved) {
                            IconTile(
                                icon = SlIcons.CheckMedium,
                                tint = SlColor.OnBrand,
                                background = SlColor.Brand,
                                size = SlDimen.TouchTarget,
                                shape = CircleShape,
                                iconSize = 22.dp,
                            )
                        } else {
                            IconTile(
                                icon = SlIcons.Moon,
                                tint = SlColor.BrandDeep,
                                background = SlColor.SurfaceAlt,
                                size = SlDimen.TouchTarget,
                                shape = RoundedCornerShape(SlDimen.RadiusField),
                                iconSize = 22.dp,
                            )
                        }
                    },
                    trailing = { if (sleepAchieved) AchievedBadge() },
                )
                SlDivider()
                ConditionRow(
                    title = stringResource(R.string.condition_pomodoro),
                    value = stringResource(
                        R.string.home_pomodoro_value,
                        stat.pomodoroSessions,
                        settings.pomodoroGoal,
                    ),
                    leading = {
                        IconTile(
                            icon = SlIcons.Timer,
                            tint = SlColor.BrandDeep,
                            background = SlColor.SurfaceAlt,
                            size = SlDimen.TouchTarget,
                            shape = RoundedCornerShape(SlDimen.RadiusField),
                            iconSize = 22.dp,
                        )
                    },
                    trailing = {
                        SessionDots(
                            completed = stat.pomodoroSessions,
                            goal = settings.pomodoroGoal,
                        )
                    },
                )
            }

            Spacer(Modifier.height(28.dp))
            SectionLabel(stringResource(R.string.home_section_blocked_apps))
            Spacer(Modifier.height(12.dp))
            SlPanel {
                lockedApps.forEachIndexed { index, app ->
                    if (index > 0) SlDivider()
                    AppListItem(
                        name = app.name,
                        badgeInitial = app.initial,
                        badgeColor = appBadgeColor(app.id),
                        subtitle = app.subtitle,
                        modifier = Modifier.clickable(
                            role = Role.Button,
                            onClick = { onAppClick(app) },
                        ),
                        trailing = { DetectingStatus() },
                    )
                }
            }
        }

        BottomNavBar(selected = selectedTab, onSelect = onTabSelected)
    }
}

@Composable
private fun AchievedBadge() {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(SlColor.BrandTintAlt)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = stringResource(R.string.home_badge_achieved),
            style = SlText.Chip,
            color = SlColor.BrandDeep,
        )
    }
}

@Composable
private fun SessionDots(completed: Int, goal: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(goal) { index ->
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (index < completed) SlColor.Brand else SlColor.Border),
            )
        }
    }
}

@Composable
private fun DetectingStatus() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(SlColor.Brand),
        )
        Text(
            text = stringResource(R.string.home_app_detecting),
            style = SlText.LabelSm,
            color = SlColor.TextSecondary,
        )
    }
}

@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun HomeScreenPreview() {
    StepLockTheme {
        HomeScreen(
            userName = SampleData.USER_NAME,
            stat = SampleData.today,
            settings = SampleData.settings,
            apps = SampleData.apps,
            selectedTab = NavTab.Home,
            onTabSelected = {},
            onManageLocks = {},
            onAppClick = {},
        )
    }
}
