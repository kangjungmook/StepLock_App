package com.steplock.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.steplock.app.R
import com.steplock.app.data.InstalledApp
import com.steplock.app.data.DailyStat
import com.steplock.app.data.LockSettings
import com.steplock.app.data.SampleData
import com.steplock.app.data.UnlockEvaluator
import com.steplock.app.ui.components.AppIcon
import com.steplock.app.ui.components.BottomNavBar
import com.steplock.app.ui.components.ConditionRow
import com.steplock.app.ui.components.MascotMood
import com.steplock.app.ui.components.NavTab
import com.steplock.app.ui.components.PrimaryButton
import com.steplock.app.ui.components.ProgressRing
import com.steplock.app.ui.components.SectionLabel
import com.steplock.app.ui.components.SlChevron
import com.steplock.app.ui.components.SlDetailRow
import com.steplock.app.ui.components.SlDivider
import com.steplock.app.ui.components.SlEmptyState
import com.steplock.app.ui.components.SlIcons
import com.steplock.app.ui.components.SlPanel
import com.steplock.app.ui.components.StepTrack
import com.steplock.app.ui.components.TextLink
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
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
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
    /** 잠그고 있는 앱. 이미 걸러진 목록입니다. */
    apps: List<InstalledApp>,
    selectedTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    onManageLocks: () -> Unit,
    onPomodoroClick: () -> Unit,
    streak: Int,
    /** 권한이 꺼져 감시가 멈춘 경우의 제목·설명. 정상이면 둘 다 null입니다. */
    warningTitle: String?,
    warningDescription: String?,
    onWarningClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** 집중 세션이 도는 중. 감시 서비스는 이 동안 조건과 무관하게 잠급니다. */
    focusing: Boolean = false,
    /** 임시 허용이 끝나는 시각(epoch ms). 이미 지났을 수 있습니다. */
    temporaryAllowUntil: Long? = null,
) {
    // 걸러는 뷰모델이 합니다 — 화면은 이름과 아이콘만 그립니다.
    val lockedApps = apps
    val conditions = enabledConditions(settings)
    val allowActive = rememberAllowActive(temporaryAllowUntil)

    // 홈의 결론은 **감시 서비스와 같은 규칙**이어야 합니다. 예전에는 조건만 봐서,
    // 5분 허용 중에도 "잠겨 있어요", 집중 중에 앱이 막혀도 "열려 있어요"라고
    // 말했습니다 — 화면과 실제가 어긋나면 사용자는 둘 다 믿지 않습니다.
    val status = when {
        allowActive -> HomeStatus.Allowed
        focusing -> HomeStatus.Focusing
        UnlockEvaluator.isUnlocked(settings, stat) -> HomeStatus.Unlocked
        else -> HomeStatus.Locked
    }
    val unlocked = status == HomeStatus.Unlocked || status == HomeStatus.Allowed

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
                    stringResource(R.string.home_greeting, stringResource(greetingRes()), userName)
                } else {
                    stringResource(greetingRes())
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

            if (conditions.isEmpty()) {
                Spacer(Modifier.height(24.dp))
                SlPanel {
                    SlEmptyState(
                        title = stringResource(R.string.home_no_conditions_title),
                        description = stringResource(R.string.home_no_conditions_desc),
                        onClick = onManageLocks,
                    )
                }
            } else {
                val hero = primaryCondition(settings)

                // 오늘의 결론을 먼저 말하고, 트랙이 근거를 보여 줍니다.
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(
                            when (status) {
                                HomeStatus.Unlocked -> R.string.home_status_unlocked_title
                                HomeStatus.Allowed -> R.string.home_status_allowed_title
                                HomeStatus.Focusing -> R.string.home_status_focusing_title
                                HomeStatus.Locked -> R.string.home_status_locked_title
                            },
                        ),
                        style = SlText.StatusTitle,
                        color = if (unlocked) SlColor.BrandInk else SlColor.AmberText,
                        modifier = Modifier.weight(1f),
                    )
                    // 하루치로는 자랑할 게 없어서 이틀 이상일 때만 보여 줍니다.
                    if (streak >= 2) StreakBadge(streak)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = when (status) {
                        HomeStatus.Unlocked -> stringResource(R.string.home_status_unlocked_desc)
                        HomeStatus.Allowed -> stringResource(
                            R.string.home_status_allowed_desc,
                            clockText(temporaryAllowUntil ?: 0L),
                        )
                        HomeStatus.Focusing -> stringResource(R.string.home_status_focusing_desc)
                        HomeStatus.Locked -> hero.remainingText(stat, settings)
                    },
                    style = SlText.BodySm,
                    color = SlColor.TextSecondary,
                )

                Spacer(Modifier.height(20.dp))
                StepTrack(
                    progress = hero.progress(stat, settings),
                    startLabel = hero.currentText(stat, settings),
                    goalLabel = stringResource(
                        R.string.home_track_goal,
                        hero.goalText(settings),
                    ),
                    mood = when (status) {
                        HomeStatus.Focusing -> MascotMood.Focusing
                        HomeStatus.Locked -> MascotMood.Walking
                        else -> MascotMood.Resting
                    },
                )

                // 집중 타이머가 맨 앞 조건이면 아래 목록에 나오지 않아서, 예전에는
                // 홈에서 타이머로 갈 길이 **아예 없었습니다.** 트랙 아래에 둡니다.
                if (hero == UnlockCondition.Pomodoro) {
                    Spacer(Modifier.height(16.dp))
                    PrimaryButton(
                        text = stringResource(
                            if (focusing) R.string.home_focus_resume else R.string.home_focus_open,
                        ),
                        onClick = onPomodoroClick,
                    )
                }

                // 트랙이 첫 조건을 보여 주므로 카드에는 나머지만 넣습니다.
                val rest = conditions.filter { it != hero }
                if (rest.isNotEmpty()) {
                    Spacer(Modifier.height(32.dp))
                    SectionLabel(stringResource(R.string.home_section_conditions_rest))
                    Spacer(Modifier.height(12.dp))
                    SlPanel {
                        rest.forEachIndexed { index, condition ->
                            if (index > 0) SlDivider()
                            ConditionRow(
                                title = stringResource(condition.titleRes),
                                value = condition.valueText(stat, settings),
                                modifier = if (condition == UnlockCondition.Pomodoro) {
                                    Modifier.clickable(
                                        role = Role.Button,
                                        onClick = onPomodoroClick,
                                    )
                                } else {
                                    Modifier
                                },
                                leading = {
                                    ConditionRing(
                                        progress = condition.progress(stat, settings),
                                        achieved = condition.isAchieved(stat, settings),
                                    )
                                },
                                trailing = if (condition == UnlockCondition.Pomodoro) {
                                    { SlChevron() }
                                } else {
                                    null
                                },
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
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
                // 목록 전체를 한 곳에서 고칩니다. 줄마다 화살표를 달면 앱마다 다른
                // 화면이 있는 것처럼 읽히는데, 실제로는 모두 같은 관리 화면입니다.
                if (lockedApps.isNotEmpty()) {
                    TextLink(
                        text = stringResource(R.string.home_manage_apps),
                        onClick = onManageLocks,
                        style = SlText.LinkSm,
                        color = SlColor.BrandInk,
                        underline = false,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
            // 앱 목록은 카드로 감싸지 않습니다. 위의 트랙·카드와 무게를 달리해
            // 같은 크기의 상자가 쌓이지 않게 합니다.
            if (lockedApps.isEmpty()) {
                SlPanel(modifier = Modifier.padding(top = 12.dp)) {
                    SlEmptyState(
                        title = stringResource(R.string.home_no_apps_title),
                        description = stringResource(R.string.home_no_apps_desc),
                        onClick = onManageLocks,
                    )
                }
            } else {
                lockedApps.forEachIndexed { index, app ->
                    if (index > 0) SlDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // 기기에서 읽은 실제 아이콘. 첫 글자 뱃지로는 "라이트" 같은
                        // 변종을 구분할 수 없습니다.
                        AppIcon(packageName = app.packageName, label = app.label, size = 36.dp)
                        Text(
                            text = app.label,
                            style = SlText.ListItem,
                            color = SlColor.TextPrimary,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        BottomNavBar(selected = selectedTab, onSelect = onTabSelected)
    }
}

/**
 * 권한이 꺼지면 잠금은 아무것도 못 하는데 화면은 평소와 같아 보입니다.
 * 그 상태를 눈에 띄게 알리고 바로 고치러 갈 수 있게 합니다.
 */
@Composable
private fun PermissionWarning(title: String, description: String, onClick: () -> Unit) {
    SlPanel(
        borderColor = SlColor.Error,
        contentPadding = PaddingValues(SlDimen.PanelPadding),
    ) {
        SlDetailRow(
            title = title,
            description = description,
            modifier = Modifier.clickable(role = Role.Button, onClick = onClick),
            titleColor = SlColor.Error,
            trailing = { SlChevron() },
        )
    }
}

/** 연속 달성 배지. 상태 제목 옆에 붙어 숫자 하나로만 자랑합니다. */
@Composable
private fun StreakBadge(streak: Int) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(SlColor.BrandTintAlt)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            text = stringResource(R.string.home_streak, streak),
            style = SlText.Chip,
            color = SlColor.BrandDeep,
        )
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

@Composable
private fun UnlockCondition.currentText(stat: DailyStat, settings: LockSettings): String =
    when (this) {
        UnlockCondition.Steps -> stringResource(
            R.string.unit_steps,
            stat.steps.formatThousands(),
        )
        UnlockCondition.Sleep -> durationLabel(stat.sleepMinutes)
        UnlockCondition.Pomodoro -> stringResource(R.string.unit_sessions, stat.pomodoroSessions)
    }

@Composable
private fun UnlockCondition.goalText(settings: LockSettings): String = when (this) {
    UnlockCondition.Steps -> stringResource(
        R.string.unit_steps,
        settings.stepGoal.formatThousands(),
    )
    UnlockCondition.Sleep -> sleepGoalLabel(settings.sleepGoalHours)
    UnlockCondition.Pomodoro -> stringResource(R.string.unit_sessions, settings.pomodoroGoal)
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
            onPomodoroClick = {},
            streak = 3,
            warningTitle = null,
            warningDescription = null,
            onWarningClick = {},
        )
    }
}

/** 다크 모드 — 같은 데이터로 색만 바뀌는지 봅니다. */
@Preview(widthDp = 412, heightDp = 892, name = "Dark")
@Composable
private fun HomeScreenPreviewDark() {
    StepLockTheme(darkTheme = true) {
        HomeScreen(
            userName = SampleData.settings.displayName,
            stat = SampleData.today,
            settings = SampleData.settings,
            apps = SampleData.apps,
            selectedTab = NavTab.Home,
            onTabSelected = {},
            onManageLocks = {},
            onPomodoroClick = {},
            streak = 3,
            warningTitle = null,
            warningDescription = null,
            onWarningClick = {},
        )
    }
}

/** 홈 맨 위 한 줄이 말하는 상태. 감시 서비스의 판단 순서와 같습니다. */
private enum class HomeStatus { Allowed, Focusing, Unlocked, Locked }

/**
 * 임시 허용이 **지금** 유효한지. 끝나는 순간 스스로 false 로 바뀝니다 —
 * 화면 상태는 설정이나 걸음이 바뀔 때만 새로 오므로, 가만히 앉아 있으면
 * 허용이 끝나도 "열려 있어요"가 남습니다.
 */
@Composable
private fun rememberAllowActive(until: Long?): Boolean {
    val active by produceState(
        initialValue = until != null && System.currentTimeMillis() < until,
        until,
    ) {
        if (until == null) return@produceState
        val left = until - System.currentTimeMillis()
        if (left > 0) {
            value = true
            delay(left)
        }
        value = false
    }
    return active
}

/** 오후 3:05 같은 시각. 남은 분을 세는 대신 끝나는 시각을 적어 매초 다시 그리지 않습니다. */
private fun clockText(epochMs: Long): String =
    Instant.ofEpochMilli(epochMs)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN))

/**
 * 시각에 맞는 인사. 예전에는 밤 11시에 열어도 "좋은 아침이에요"였습니다 —
 * 첫 줄이 틀리면 그 아래 숫자도 덜 믿게 됩니다.
 */
private fun greetingRes(hour: Int = LocalTime.now().hour): Int = when (hour) {
    in 5..10 -> R.string.home_greeting_morning
    in 11..16 -> R.string.home_greeting_afternoon
    in 17..21 -> R.string.home_greeting_evening
    else -> R.string.home_greeting_night
}
