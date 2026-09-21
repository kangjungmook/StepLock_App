package com.steplock.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Icon
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
import com.steplock.app.data.UnlockEvaluator
import com.steplock.app.ui.components.AppListItem
import com.steplock.app.ui.components.BottomNavBar
import com.steplock.app.ui.components.ConditionRow
import com.steplock.app.ui.components.MascotMood
import com.steplock.app.ui.components.NavTab
import com.steplock.app.ui.components.ProgressRing
import com.steplock.app.ui.components.SectionLabel
import com.steplock.app.ui.components.SlDivider
import com.steplock.app.ui.components.SlIcons
import com.steplock.app.ui.components.SlPanel
import com.steplock.app.ui.components.StepLockMascot
import com.steplock.app.ui.components.appBadgeColor
import com.steplock.app.ui.theme.SlColor
import com.steplock.app.ui.theme.SlDimen
import com.steplock.app.ui.theme.SlText
import com.steplock.app.ui.theme.StepLockTheme
import com.steplock.app.ui.util.UnlockCondition
import com.steplock.app.ui.util.durationLabel
import com.steplock.app.ui.util.enabledConditions
import com.steplock.app.ui.util.formatThousands
import com.steplock.app.ui.util.isAchieved
import com.steplock.app.ui.util.primaryCondition
import com.steplock.app.ui.util.progress
import com.steplock.app.ui.util.remainingText
import com.steplock.app.ui.util.sleepGoalLabel
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/**
 * 홈. 화면을 열었을 때 가장 알고 싶은 것은 "지금 잠겨 있나"이므로
 * 그 답을 맨 위 한 장으로 보여 주고, 조건은 같은 모양의 링으로 나열합니다.
 */
@Composable
fun HomeScreen(
    userName: String?,
    stat: DailyStat,
    settings: LockSettings,
    apps: List<BlockedApp>,
    selectedTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    onManageLocks: () -> Unit,
    onAppClick: (BlockedApp) -> Unit,
    onPomodoroClick: () -> Unit,
    streak: Int,
    /** 권한이 꺼져 감시가 멈춘 경우의 제목·설명. 정상이면 둘 다 null입니다. */
    warningTitle: String?,
    warningDescription: String?,
    onWarningClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lockedApps = apps.filter { it.id in settings.blockedAppIds }
    val conditions = enabledConditions(settings)
    val unlocked = UnlockEvaluator.isUnlocked(settings, stat)

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
                text = if (userName != null) {
                    stringResource(R.string.home_greeting, userName)
                } else {
                    stringResource(R.string.home_greeting_anonymous)
                },
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

            if (warningTitle != null && warningDescription != null) {
                Spacer(Modifier.height(20.dp))
                PermissionWarning(
                    title = warningTitle,
                    description = warningDescription,
                    onClick = onWarningClick,
                )
            }

            Spacer(Modifier.height(24.dp))
            TodayStatusCard(
                unlocked = unlocked,
                description = if (unlocked) {
                    stringResource(R.string.home_status_unlocked_desc)
                } else {
                    primaryCondition(settings).remainingText(stat, settings)
                },
                streak = streak,
            )

            Spacer(Modifier.height(28.dp))
            SectionLabel(stringResource(R.string.home_section_conditions))
            Spacer(Modifier.height(12.dp))
            SlPanel {
                if (conditions.isEmpty()) {
                    EmptyHint(
                        title = stringResource(R.string.home_no_conditions_title),
                        description = stringResource(R.string.home_no_conditions_desc),
                        onClick = onManageLocks,
                    )
                } else {
                    conditions.forEachIndexed { index, condition ->
                        if (index > 0) SlDivider()
                        ConditionRow(
                            title = stringResource(condition.titleRes),
                            value = condition.valueText(stat, settings),
                            modifier = if (condition == UnlockCondition.Pomodoro) {
                                Modifier.clickable(role = Role.Button, onClick = onPomodoroClick)
                            } else {
                                Modifier
                            },
                            leading = {
                                // 세 조건 모두 "목표 대비 진행"이라 같은 링으로 보여 줍니다.
                                ConditionRing(
                                    progress = condition.progress(stat, settings),
                                    achieved = condition.isAchieved(stat, settings),
                                )
                            },
                            trailing = if (condition == UnlockCondition.Pomodoro) {
                                { Chevron() }
                            } else {
                                null
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionLabel(
                    text = stringResource(R.string.home_section_blocked_apps),
                    modifier = Modifier.weight(1f),
                )
                // 감지는 앱별 상태가 아니라 하나뿐인 감시 서비스의 상태입니다.
                if (lockedApps.isNotEmpty() && warningTitle == null) DetectingStatus()
            }
            Spacer(Modifier.height(12.dp))
            SlPanel {
                if (lockedApps.isEmpty()) {
                    EmptyHint(
                        title = stringResource(R.string.home_no_apps_title),
                        description = stringResource(R.string.home_no_apps_desc),
                        onClick = onManageLocks,
                    )
                } else {
                    lockedApps.forEachIndexed { index, app ->
                        if (index > 0) SlDivider()
                        AppListItem(
                            name = app.name,
                            badgeInitial = app.initial,
                            badgeColor = appBadgeColor(app.id),
                            subtitle = app.subtitle,
                            badgeSize = 36.dp,
                            nameStyle = SlText.ListItem,
                            verticalPadding = 12.dp,
                            modifier = Modifier.clickable(
                                role = Role.Button,
                                onClick = { onAppClick(app) },
                            ),
                            trailing = { Chevron() },
                        )
                    }
                }
            }
        }

        BottomNavBar(selected = selectedTab, onSelect = onTabSelected)
    }
}

/**
 * 오늘 잠금이 풀렸는지 한 장으로. 상태에 따라 색과 캐릭터 표정이 함께 바뀌어서
 * 글을 읽지 않아도 구분됩니다.
 */
@Composable
private fun TodayStatusCard(unlocked: Boolean, description: String, streak: Int) {
    val container = if (unlocked) SlColor.BrandTint else SlColor.AmberSurface
    val titleColor = if (unlocked) SlColor.BrandDeep else SlColor.AmberText
    val descColor = if (unlocked) SlColor.BrandInk else SlColor.AmberSubText

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SlDimen.RadiusPanel))
            .background(container)
            .padding(start = 16.dp, end = 20.dp, top = 16.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepLockMascot(
            modifier = Modifier.size(width = 60.dp, height = 74.dp),
            mood = if (unlocked) MascotMood.Resting else MascotMood.Walking,
            footprintColor = descColor.copy(alpha = 0.28f),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(
                    if (unlocked) {
                        R.string.home_status_unlocked_title
                    } else {
                        R.string.home_status_locked_title
                    },
                ),
                style = SlText.StatusTitle,
                color = titleColor,
            )
            Spacer(Modifier.height(6.dp))
            Text(text = description, style = SlText.BodySm, color = descColor)

            // 하루치로는 자랑할 게 없어서 이틀 이상일 때만 보여 줍니다.
            if (streak >= 2) {
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(SlColor.Surface)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(
                        text = stringResource(R.string.home_streak, streak),
                        style = SlText.Chip,
                        color = titleColor,
                    )
                }
            }
        }
    }
}

/**
 * 권한이 꺼지면 잠금은 아무것도 못 하는데 화면은 평소와 같아 보입니다.
 * 그 상태를 눈에 띄게 알리고 바로 고치러 갈 수 있게 합니다.
 */
@Composable
private fun PermissionWarning(title: String, description: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SlDimen.RadiusCard))
            .background(SlColor.Surface)
            .border(1.dp, SlColor.Error, RoundedCornerShape(SlDimen.RadiusCard))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = SlText.RowTitle, color = SlColor.Error)
            Text(
                text = description,
                style = SlText.RowValue,
                color = SlColor.TextSecondary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Chevron()
    }
}

/** 달성하면 숫자 대신 체크가 들어갑니다 — 모양은 그대로 두고 상태만 바꿉니다. */
@Composable
private fun ConditionRing(progress: Float, achieved: Boolean) {
    ProgressRing(
        progress = progress,
        size = SlDimen.TouchTarget,
        radius = 18.dp,
        strokeWidth = 4.dp,
    ) {
        if (achieved) {
            Icon(
                imageVector = SlIcons.CheckBold,
                contentDescription = null,
                tint = SlColor.Brand,
                modifier = Modifier.size(18.dp),
            )
        } else {
            Text(
                text = "${(progress * 100).roundToInt()}%",
                style = SlText.RingPercent,
                color = SlColor.BrandDeep,
            )
        }
    }
}

@Composable
private fun Chevron() {
    Icon(
        imageVector = SlIcons.ChevronRight,
        contentDescription = null,
        tint = SlColor.TextTertiary,
        modifier = Modifier.size(18.dp),
    )
}

/** 조건이나 앱을 하나도 고르지 않았을 때. 빈 패널을 두지 않고 다음 행동을 안내합니다. */
@Composable
private fun EmptyHint(title: String, description: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = SlText.RowTitle, color = SlColor.TextPrimary)
            Text(
                text = description,
                style = SlText.RowValue,
                color = SlColor.TextSecondary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Chevron()
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

@Composable
private fun UnlockCondition.valueText(stat: DailyStat, settings: LockSettings): String = when (this) {
    UnlockCondition.Steps -> stringResource(
        R.string.home_steps_value,
        stat.steps.formatThousands(),
        settings.stepGoal.formatThousands(),
    )
    UnlockCondition.Sleep -> stringResource(
        R.string.home_sleep_value,
        durationLabel(stat.sleepMinutes),
        sleepGoalLabel(settings.sleepGoalHours),
    )
    UnlockCondition.Pomodoro -> stringResource(
        R.string.home_pomodoro_value,
        stat.pomodoroSessions,
        settings.pomodoroGoal,
    )
}

@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun HomeScreenPreview() {
    StepLockTheme {
        HomeScreen(
            userName = SampleData.settings.displayName,
            stat = SampleData.today,
            settings = SampleData.settings,
            apps = SampleData.apps,
            selectedTab = NavTab.Home,
            onTabSelected = {},
            onManageLocks = {},
            onAppClick = {},
            onPomodoroClick = {},
            streak = 3,
            warningTitle = null,
            warningDescription = null,
            onWarningClick = {},
        )
    }
}
