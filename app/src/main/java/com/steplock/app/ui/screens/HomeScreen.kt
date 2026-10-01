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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.steplock.app.R
import com.steplock.app.data.DailyStat
import com.steplock.app.data.InstalledApp
import com.steplock.app.data.LockSettings
import com.steplock.app.data.Pomodoro
import com.steplock.app.data.SampleData
import com.steplock.app.data.TemporaryAllow
import com.steplock.app.data.UnlockEvaluator
import com.steplock.app.ui.components.AppIcon
import com.steplock.app.ui.components.BottomNavBar
import com.steplock.app.ui.components.BubbleTail
import com.steplock.app.ui.components.ConditionRow
import com.steplock.app.ui.components.IconTile
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
import com.steplock.app.ui.components.StepLockMascot
import com.steplock.app.ui.components.StepTrack
import com.steplock.app.ui.components.StepiSays
import com.steplock.app.ui.components.WeeklyBarChart
import com.steplock.app.ui.theme.SlColor
import com.steplock.app.ui.theme.SlDimen
import com.steplock.app.ui.theme.SlText
import com.steplock.app.ui.theme.StepLockTheme
import com.steplock.app.ui.util.UnlockCondition
import com.steplock.app.ui.util.durationLabel
import com.steplock.app.ui.util.enabledConditions
import com.steplock.app.ui.util.formatThousands
import com.steplock.app.ui.util.isAchieved
import com.steplock.app.ui.util.minutesLabel
import com.steplock.app.ui.util.numeralText
import com.steplock.app.ui.util.primaryCondition
import com.steplock.app.ui.util.progress
import com.steplock.app.ui.util.sleepGoalLabel
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * 홈. **할 일의 양을 가장 크게** 말하고, 그 아래로 길 → 나머지 조건 → 오늘 한눈에 →
 * 최근 7일 → 오늘 막은 앱 순서입니다.
 *
 * 1. 지금: 어떤 앱이 잠겼는지 한 줄, 그리고 큰 숫자 "2,760보" + "더 걸으면 잠금이 풀려요".
 *    화면을 연 사람이 가장 알고 싶은 "얼마나 더"를 제일 먼저, 제일 크게 답합니다.
 * 2. 길: 스텝이가 굵은 길 위를 걸어가고, 끝에는 자물쇠가 있습니다(2k · 4k · 6k 이정표).
 * 3. 나머지 조건: 옅은 띠 한 줄씩("수면 7시간 채웠어요 ✓").
 * 4. 오늘 한눈에: 숫자 넷을 2×2로. 칸은 선으로만 나눕니다.
 * 5. 최근 7일: 걸음 막대와 목표 점선. 채운 날은 진한 색, 오늘은 가장 진한 색.
 * 6. 오늘 막은 앱: 앱마다 막은 횟수만큼 차는 막대.
 *
 * 카드를 쌓지 않습니다. 구역은 44dp 여백과 섹션 제목으로 나누고, 면을 쓰는 건
 * 나머지 조건 띠뿐입니다. 하단 탭은 내용 위에 떠 있고, 내용은 그 뒤로 흐릅니다.
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
    /** 오늘까지 7일. 마지막 항목이 오늘입니다. */
    weekly: List<DailyStat> = emptyList(),
    /** 오늘 잠금 화면으로 막은 횟수 — 패키지 이름별. */
    blockedToday: Map<String, Int> = emptyMap(),
    /** 오늘 더 쓸 수 있는 임시 허용 횟수. */
    temporaryAllowRemaining: Int = TemporaryAllow.DAILY_LIMIT,
    /** 오늘 잠근 앱별 사용 시간(ms). */
    usageToday: Map<String, Long> = emptyMap(),
) {
    val conditions = enabledConditions(settings)
    val allowActive = rememberAllowActive(temporaryAllowUntil)

    // 홈의 결론은 **감시 서비스와 같은 규칙**이어야 합니다. 화면과 실제가 어긋나면
    // 사용자는 둘 다 믿지 않습니다. 집중이 허용보다 먼저입니다 — 감시 서비스도
    // 집중 중에는 5분 허용을 듣지 않습니다.
    val status = when {
        apps.isEmpty() && !focusing -> HomeStatus.Idle
        focusing -> HomeStatus.Focusing
        allowActive -> HomeStatus.Allowed
        UnlockEvaluator.isUnlocked(settings, stat) -> HomeStatus.Unlocked
        else -> HomeStatus.Locked
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SlColor.Background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = SlDimen.ScreenPadding,
                    end = SlDimen.ScreenPadding,
                    top = 12.dp,
                    bottom = SlDimen.FloatingNavReserve,
                )
                .navigationBarsPadding(),
        ) {
            HomeHeader(userName = userName, stat = stat, streak = streak)

            if (warningTitle != null && warningDescription != null) {
                Spacer(Modifier.height(20.dp))
                PermissionWarning(
                    title = warningTitle,
                    description = warningDescription,
                    onClick = onWarningClick,
                )
            }

            when {
                status == HomeStatus.Idle -> {
                    // 잠근 앱이 없으면 막을 게 없으니 조건도 진행하지 않습니다.
                    IdleHero(onPickApps = onManageLocks)
                    if (conditions.isNotEmpty()) {
                        HomeSectionHeader(
                            title = stringResource(R.string.home_section_conditions_preview),
                            trailing = {
                                SectionLink(
                                    text = stringResource(R.string.home_conditions_edit),
                                    onClick = { onTabSelected(NavTab.Settings) },
                                )
                            },
                        )
                        ConditionPreview(conditions = conditions, settings = settings)
                    }
                }

                conditions.isEmpty() -> {
                    Spacer(Modifier.height(24.dp))
                    SlPanel {
                        // 조건은 설정 탭에서 켭니다.
                        SlEmptyState(
                            title = stringResource(R.string.home_no_conditions_title),
                            description = stringResource(R.string.home_no_conditions_desc),
                            onClick = { onTabSelected(NavTab.Settings) },
                        )
                    }
                }

                else -> LockingContent(
                    status = status,
                    stat = stat,
                    settings = settings,
                    apps = apps,
                    conditions = conditions,
                    streak = streak,
                    focusing = focusing,
                    temporaryAllowUntil = temporaryAllowUntil,
                    weekly = weekly,
                    blockedToday = blockedToday,
                    temporaryAllowRemaining = temporaryAllowRemaining,
                    usageToday = usageToday,
                    detecting = warningTitle == null,
                    onPomodoroClick = onPomodoroClick,
                    onManageLocks = onManageLocks,
                    onOpenStats = { onTabSelected(NavTab.Stats) },
                )
            }
        }

        BottomNavBar(
            selected = selectedTab,
            onSelect = onTabSelected,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/** 앱을 잠근 뒤의 홈 — 큰 숫자부터 오늘 막은 앱까지. */
@Composable
private fun LockingContent(
    status: HomeStatus,
    stat: DailyStat,
    settings: LockSettings,
    apps: List<InstalledApp>,
    conditions: List<UnlockCondition>,
    streak: Int,
    focusing: Boolean,
    temporaryAllowUntil: Long?,
    weekly: List<DailyStat>,
    blockedToday: Map<String, Int>,
    temporaryAllowRemaining: Int,
    usageToday: Map<String, Long>,
    detecting: Boolean,
    onPomodoroClick: () -> Unit,
    onManageLocks: () -> Unit,
    onOpenStats: () -> Unit,
) {
    val hero = primaryCondition(settings)

    // 1. 지금 — 무엇이 잠겼고, 얼마나 더 하면 되는지.
    Spacer(Modifier.height(32.dp))
    StatusLine(status = status, apps = apps)
    Spacer(Modifier.height(12.dp))
    BigHero(
        status = status,
        hero = hero,
        stat = stat,
        settings = settings,
        allowUntil = temporaryAllowUntil,
    )

    // 2. 길 — 스텝이가 목표까지 걸어갑니다.
    Spacer(Modifier.height(28.dp))
    StepTrack(
        progress = hero.progress(stat, settings),
        startLabel = hero.currentText(stat, settings),
        goalLabel = stringResource(R.string.home_track_goal, hero.goalText(settings)),
        mood = when (status) {
            HomeStatus.Focusing -> MascotMood.Focusing
            HomeStatus.Locked -> MascotMood.Walking
            else -> MascotMood.Resting
        },
        ticks = if (hero == UnlockCondition.Steps) stepTicks(settings.stepGoal) else emptyList(),
    )

    // 집중 타이머가 맨 앞 조건이면 타이머로 가는 길을 길 바로 아래에 둡니다.
    if (hero == UnlockCondition.Pomodoro) {
        Spacer(Modifier.height(20.dp))
        PrimaryButton(
            text = stringResource(
                if (focusing) R.string.home_focus_resume else R.string.home_focus_open,
            ),
            onClick = onPomodoroClick,
        )
    }

    // 3. 나머지 조건 — 옅은 띠 한 줄씩.
    val rest = conditions.filter { it != hero }
    if (rest.isNotEmpty()) {
        Spacer(Modifier.height(24.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            rest.forEach { condition ->
                ConditionStrip(
                    condition = condition,
                    stat = stat,
                    settings = settings,
                    onClick = if (condition == UnlockCondition.Pomodoro) onPomodoroClick else null,
                )
            }
        }
    }

    // 4. 오늘 한눈에 — 잠근 앱을 얼마나 썼고 얼마나 잠겨 있었는지를 맨 앞에.
    HomeSectionHeader(stringResource(R.string.home_section_glance))
    val achievedDays = weekly.count { UnlockEvaluator.isUnlocked(settings, it) }
    GlanceGrid(
        figures = listOf(
            minutesLabel(stat.blockedUsageMinutes) to stringResource(R.string.home_glance_usage),
            minutesLabel(stat.lockedMinutes) to stringResource(R.string.home_glance_locked),
            stringResource(R.string.home_times, blockedToday.values.sum()) to
                stringResource(R.string.home_glance_blocked),
            distanceText(stat.steps) to stringResource(R.string.home_today_distance),
            stringResource(R.string.home_times, temporaryAllowRemaining) to
                stringResource(R.string.home_glance_allow_left, TemporaryAllow.MINUTES),
            if (settings.pomodoroEnabled) {
                stringResource(
                    R.string.home_glance_focus_value,
                    stat.pomodoroSessions,
                    settings.pomodoroGoal,
                ) to stringResource(R.string.home_glance_focus)
            } else {
                stringResource(R.string.home_glance_week_value, achievedDays, weekly.size) to
                    stringResource(R.string.home_glance_week)
            },
        ),
    )

    // 5. 최근 7일
    if (weekly.isNotEmpty()) {
        HomeSectionHeader(
            title = stringResource(
                if (settings.stepsEnabled) R.string.home_section_week_steps else R.string.home_section_week,
            ),
            trailing = {
                SectionLink(
                    text = stringResource(R.string.home_week_achieved, achievedDays),
                    onClick = onOpenStats,
                )
            },
        )
        if (settings.stepsEnabled) {
            WeekBars(days = weekly, settings = settings, onClick = onOpenStats)
        } else {
            WeekRings(days = weekly, settings = settings, hero = hero, onClick = onOpenStats)
        }
    }

    // 6. 오늘 막은 앱
    HomeSectionHeader(
        title = stringResource(R.string.home_section_blocked_apps),
        trailing = {
            // 감지는 앱별 상태가 아니라 하나뿐인 감시 서비스의 상태입니다.
            if (detecting) DetectingStatus()
            SectionLink(
                text = stringResource(R.string.home_manage_apps),
                onClick = onManageLocks,
                chevron = false,
            )
        },
    )
    // 오래 쓴 앱이 위로, 같으면 많이 막은 앱이 위로. 같은 값끼리는 이름 순을 지킵니다.
    val sorted = apps.sortedWith(
        compareByDescending<InstalledApp> { usageToday[it.packageName] ?: 0L }
            .thenByDescending { blockedToday[it.packageName] ?: 0 },
    )
    val maxUsage = sorted.maxOfOrNull { usageToday[it.packageName] ?: 0L }?.coerceAtLeast(1L) ?: 1L
    sorted.forEachIndexed { index, app ->
        if (index > 0) SlDivider()
        BlockedAppRow(
            app = app,
            usageMs = usageToday[app.packageName] ?: 0L,
            maxUsageMs = maxUsage,
            blockedCount = blockedToday[app.packageName] ?: 0,
        )
    }
    Spacer(Modifier.height(8.dp))
    // 쇼츠만 따로 재지 못한다는 걸 숨기지 않습니다 — 유튜브 시간은 앱 전체입니다.
    Text(
        text = stringResource(R.string.home_usage_note),
        style = SlText.Caption,
        color = SlColor.TextTertiary,
    )
}

/** 날짜와 이름 한 줄, 오른쪽에 연속 달성. 큰 숫자가 주인공이라 인사는 작게 둡니다. */
@Composable
private fun HomeHeader(userName: String?, stat: DailyStat, streak: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val date = stat.date.format(
            DateTimeFormatter.ofPattern(stringResource(R.string.home_date_pattern), Locale.KOREAN),
        )
        Text(
            text = if (userName != null) {
                stringResource(R.string.home_header_with_name, date, userName)
            } else {
                date
            },
            style = SlText.RowValue,
            color = SlColor.TextSecondary,
            modifier = Modifier.weight(1f),
        )
        // 하루치로는 자랑할 게 없어서 이틀 이상일 때만 보여 줍니다.
        if (streak >= 2) {
            Text(
                text = stringResource(R.string.home_streak, streak),
                style = SlText.Chip,
                color = SlColor.BrandDeep,
            )
        }
    }
}

/** 섹션 제목 줄. 섹션 사이는 카드 대신 이 여백(44)으로 나눕니다. */
@Composable
private fun HomeSectionHeader(
    title: String,
    trailing: (@Composable () -> Unit)? = null,
) {
    Spacer(Modifier.height(44.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionLabel(text = title, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
    Spacer(Modifier.height(12.dp))
}

/** "5일 달성 ›" 같은 섹션 오른쪽 링크. 44dp 터치 높이를 지킵니다. */
@Composable
private fun SectionLink(text: String, onClick: () -> Unit, chevron: Boolean = true) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(SlDimen.RadiusSmall))
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = SlDimen.TouchTarget)
            .padding(start = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = text, style = SlText.LinkSm, color = SlColor.BrandInk)
        if (chevron) SlChevron(tint = SlColor.BrandInk, modifier = Modifier.padding(start = 2.dp))
    }
}

/**
 * 무엇이 잠겼는지 — 표지판 판 하나. 잠김은 **흑연 판에 흰 글자와 자물쇠**, 열림·허용·
 * 집중은 주황 판, 잠근 앱 없음은 회색 판입니다. 색 하나로 상태가 먼저 읽힙니다.
 */
@Composable
private fun StatusLine(status: HomeStatus, apps: List<InstalledApp>) {
    val (container, content) = when (status) {
        HomeStatus.Locked -> SlColor.TextPrimary to SlColor.Background
        HomeStatus.Idle -> SlColor.SurfaceAlt to SlColor.TextSecondary
        else -> SlColor.Brand to SlColor.OnBrand
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(SlDimen.RadiusSmall))
            .background(container)
            .heightIn(min = 32.dp)
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (status == HomeStatus.Locked) {
            Icon(
                imageVector = SlIcons.PasswordLock,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(14.dp),
            )
        }
        Text(
            text = when (status) {
                HomeStatus.Locked -> when {
                    apps.size == 1 -> stringResource(R.string.home_status_locked_one, apps[0].label)
                    apps.isNotEmpty() -> stringResource(
                        R.string.home_status_locked_many,
                        apps[0].label,
                        apps.size - 1,
                    )
                    else -> stringResource(R.string.home_pill_locked)
                }
                HomeStatus.Unlocked -> stringResource(R.string.home_pill_unlocked)
                HomeStatus.Allowed -> stringResource(R.string.home_pill_allowed)
                HomeStatus.Focusing -> stringResource(R.string.home_pill_focusing)
                HomeStatus.Idle -> stringResource(R.string.home_pill_idle)
            },
            style = SlText.Chip,
            color = content,
        )
    }
}

/**
 * 홈의 주인공 — 표지판 숫자 하나와 그 뜻을 푸는 한 줄.
 *
 * 숫자는 도로 표지판에서 온 좁은 숫자체(88sp)로, 단위("보", "번", "오후")는 본문
 * 글꼴로 작게 붙입니다. 이정표의 "2.7 km" 처럼 숫자가 먼저, 단위는 곁에.
 * 숫자가 없는 상태(열림·집중·수면)는 짧은 말을 굵게 씁니다.
 */
@Composable
private fun BigHero(
    status: HomeStatus,
    hero: UnlockCondition,
    stat: DailyStat,
    settings: LockSettings,
    allowUntil: Long?,
) {
    var number: String? = null
    var unit: String? = null
    // "오후 3:05" 처럼 단위가 숫자 앞에 오는 경우.
    var unitFirst = false
    var word: String? = null
    val line: String
    var sub: String? = null
    when (status) {
        HomeStatus.Locked -> when {
            // 전부 만족 모드에서 맨 앞 조건은 채웠지만 다른 조건이 남은 경우.
            hero.isAchieved(stat, settings) -> {
                word = stringResource(R.string.home_big_almost)
                line = stringResource(R.string.home_line_rest)
            }
            hero == UnlockCondition.Steps -> {
                val remaining = (settings.stepGoal - stat.steps).coerceAtLeast(0)
                number = remaining.formatThousands()
                unit = stringResource(R.string.home_unit_steps)
                line = stringResource(R.string.home_line_steps)
                // 보통 걸음(분당 약 100보)으로 몇 분이면 되는지. 너무 길면 나눠 걸으라고.
                val minutes = ceil(remaining / STEPS_PER_MINUTE).toInt().coerceAtLeast(1)
                sub = if (minutes <= 90) {
                    stringResource(R.string.home_sub_steps_minutes, minutes)
                } else {
                    stringResource(R.string.home_sub_steps_long)
                }
            }
            // 지금 당장 채울 수 없는 조건이라 숫자로 재촉하지 않습니다.
            hero == UnlockCondition.Sleep -> {
                word = stringResource(R.string.home_big_sleep)
                line = stringResource(R.string.home_line_sleep)
            }
            else -> {
                number = (settings.pomodoroGoal - stat.pomodoroSessions).coerceAtLeast(1).toString()
                unit = stringResource(R.string.home_unit_times)
                line = stringResource(R.string.home_line_pomodoro)
                sub = stringResource(R.string.home_sub_pomodoro, Pomodoro.SESSION_MINUTES)
            }
        }
        HomeStatus.Unlocked -> {
            word = stringResource(R.string.home_big_unlocked)
            line = stringResource(R.string.home_line_unlocked)
        }
        HomeStatus.Allowed -> {
            val time = Instant.ofEpochMilli(allowUntil ?: 0L).atZone(ZoneId.systemDefault())
            number = time.format(DateTimeFormatter.ofPattern("h:mm", Locale.KOREAN))
            unit = time.format(DateTimeFormatter.ofPattern("a", Locale.KOREAN))
            unitFirst = true
            line = stringResource(R.string.home_line_allowed)
        }
        HomeStatus.Focusing -> {
            word = stringResource(R.string.home_big_focusing)
            line = stringResource(R.string.home_line_focusing)
        }
        HomeStatus.Idle -> {
            line = stringResource(R.string.home_hero_idle)
        }
    }
    Column(modifier = Modifier.semantics(mergeDescendants = true) {}) {
        if (number != null) {
            Text(
                text = buildAnnotatedString {
                    if (unit != null && unitFirst) {
                        withStyle(SlText.HomeUnit.toSpanStyle()) { append("$unit ") }
                    }
                    append(number)
                    if (unit != null && !unitFirst) {
                        withStyle(SlText.HomeUnit.toSpanStyle()) { append(" $unit") }
                    }
                },
                style = SlText.HomeNumeral,
                color = SlColor.TextPrimary,
            )
        } else if (word != null) {
            Text(text = word, style = SlText.HomeBig, color = SlColor.TextPrimary)
        }
        Spacer(Modifier.height(8.dp))
        Text(text = line, style = SlText.HomeLine, color = SlColor.TextPrimary)
        if (sub != null) {
            Spacer(Modifier.height(8.dp))
            Text(text = sub, style = SlText.Body, color = SlColor.TextSecondary)
        }
    }
}

/**
 * 나머지 조건 한 줄. 옅은 면(SurfaceAlt) 띠 — 홈에서 면을 쓰는 유일한 곳이라,
 * 카드처럼 테두리를 두르지 않고 색만 살짝 깔아 "곁가지"로 읽히게 합니다.
 */
@Composable
private fun ConditionStrip(
    condition: UnlockCondition,
    stat: DailyStat,
    settings: LockSettings,
    onClick: (() -> Unit)?,
) {
    val achieved = condition.isAchieved(stat, settings)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SlDimen.RadiusCta))
            .background(SlColor.SurfaceAlt)
            .then(
                if (onClick != null) {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .heightIn(min = 52.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = condition.icon(),
            contentDescription = null,
            tint = if (achieved) SlColor.BrandDeep else SlColor.TextSecondary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = if (achieved) {
                stringResource(R.string.home_strip_done, condition.doneText(settings))
            } else {
                stringResource(condition.titleRes)
            },
            style = SlText.RowTitle,
            color = SlColor.TextPrimary,
            modifier = Modifier.weight(1f),
        )
        if (achieved) {
            Icon(
                imageVector = SlIcons.CheckBold,
                contentDescription = null,
                tint = SlColor.Brand,
                modifier = Modifier.size(18.dp),
            )
        } else {
            Text(
                text = condition.valueText(stat, settings),
                style = SlText.RowValue,
                color = SlColor.TextSecondary,
            )
            if (onClick != null) SlChevron()
        }
    }
}

/** 오늘의 숫자 넷을 2×2로. 칸 사이는 선 하나씩만. */
@Composable
private fun GlanceGrid(figures: List<Pair<String, String>>) {
    Column {
        figures.chunked(2).forEachIndexed { rowIndex, row ->
            if (rowIndex > 0) SlDivider()
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEachIndexed { colIndex, (value, label) ->
                    if (colIndex > 0) {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(GLANCE_CELL)
                                .background(SlColor.Border),
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(GLANCE_CELL)
                            .padding(start = if (colIndex > 0) 20.dp else 0.dp)
                            .semantics(mergeDescendants = true) {},
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = numeralText(value, unitSize = 15.sp),
                            style = SlText.GlanceValue,
                            color = SlColor.TextPrimary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(text = label, style = SlText.LabelSm, color = SlColor.TextSecondary)
                    }
                }
            }
        }
    }
}

/**
 * 최근 7일 걸음 막대와 목표 점선. 채운 날은 브랜드, 못 채운 날은 옅은 브랜드,
 * 오늘은 가장 진한 색 — 오늘이 어디쯤인지가 먼저 보입니다.
 */
@Composable
private fun WeekBars(days: List<DailyStat>, settings: LockSettings, onClick: () -> Unit) {
    val todayLabel = stringResource(R.string.home_week_today)
    // 채운 날은 흑연, 못 채운 날은 흐린 회색, 오늘만 표식 주황 — "지금 여기"를 가리킵니다.
    val done = SlColor.TextPrimary
    val notYet = SlColor.TrackOff
    val today = SlColor.Brand
    val description = stringResource(
        R.string.home_week_bars_desc,
        days.count { it.steps >= settings.stepGoal },
        days.size,
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SlDimen.RadiusField))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        Text(
            text = stringResource(R.string.home_week_goal_line, settings.stepGoal.formatThousands()),
            style = SlText.LabelSm,
            color = SlColor.TextTertiary,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(4.dp))
        WeeklyBarChart(
            values = days.map { it.steps },
            goal = settings.stepGoal,
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp),
            barColorAt = { index, value ->
                when {
                    index == days.lastIndex -> today
                    value >= settings.stepGoal -> done
                    else -> notYet
                }
            },
        )
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            days.forEachIndexed { index, day ->
                val isToday = index == days.lastIndex
                Text(
                    text = if (isToday) {
                        todayLabel
                    } else {
                        day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)
                    },
                    style = if (isToday) SlText.Chip else SlText.LabelSm,
                    color = if (isToday) SlColor.BrandDeep else SlColor.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * 걸음 조건을 끈 경우의 7일 — 요일마다 링. 채운 날은 꽉 찬 원에 체크.
 * 달성 여부는 연속 기록·통계와 같은 기준(지금 설정)으로 판정합니다.
 */
@Composable
private fun WeekRings(
    days: List<DailyStat>,
    settings: LockSettings,
    hero: UnlockCondition,
    onClick: () -> Unit,
) {
    val todayLabel = stringResource(R.string.home_week_today)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SlDimen.RadiusField))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 4.dp),
    ) {
        days.forEachIndexed { index, day ->
            val isToday = index == days.lastIndex
            val done = UnlockEvaluator.isUnlocked(settings, day)
            val progress = hero.progress(day, settings)
            val dayName = day.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.KOREAN)
            val description = if (done) {
                stringResource(R.string.home_week_day_done, dayName)
            } else {
                stringResource(R.string.home_week_day_progress, dayName, (progress * 100).roundToInt())
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .semantics(mergeDescendants = true) { contentDescription = description },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (done) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SlColor.Brand),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = SlIcons.CheckBold,
                            contentDescription = null,
                            tint = SlColor.OnBrand,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                } else {
                    ProgressRing(progress = progress, size = 40.dp, radius = 18.dp, strokeWidth = 4.dp)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (isToday) {
                        todayLabel
                    } else {
                        day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)
                    },
                    style = if (isToday) SlText.Chip else SlText.LabelSm,
                    color = if (isToday) SlColor.BrandDeep else SlColor.TextSecondary,
                )
            }
        }
    }
}

/**
 * 잠근 앱 한 줄 — 아이콘, 이름과 "3번 막았어요", 오늘 쓴 시간만큼 차는 막대,
 * 오른쪽에 쓴 시간. 막대는 오늘 가장 오래 쓴 앱을 끝으로 둔 상대 길이입니다.
 */
@Composable
private fun BlockedAppRow(app: InstalledApp, usageMs: Long, maxUsageMs: Long, blockedCount: Int) {
    val usageMinutes = (usageMs / 60_000L).toInt()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 기기에서 읽은 실제 아이콘. 첫 글자 뱃지로는 "라이트" 같은 변종을 구분할 수 없습니다.
        AppIcon(packageName = app.packageName, label = app.label, size = 40.dp)
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = app.label,
                    style = SlText.ListItem,
                    color = SlColor.TextPrimary,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (blockedCount > 0) {
                        stringResource(R.string.home_app_blocked_times, blockedCount)
                    } else {
                        stringResource(R.string.home_app_blocked_zero)
                    },
                    style = SlText.LabelSm,
                    color = if (blockedCount > 0) SlColor.AmberText else SlColor.TextTertiary,
                )
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(SlColor.SurfaceAlt),
            ) {
                if (usageMs > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((usageMs.toFloat() / maxUsageMs).coerceIn(0.02f, 1f))
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(SlColor.Amber),
                    )
                }
            }
        }
        Text(
            text = minutesLabel(usageMinutes),
            style = SlText.Remaining,
            color = if (usageMinutes > 0) SlColor.TextPrimary else SlColor.TextTertiary,
            textAlign = TextAlign.End,
            modifier = Modifier.width(64.dp),
        )
    }
}

/**
 * 잠근 앱이 없을 때의 첫 구역. 진행 트랙 대신 스텝이가 할 일을 알려 주고,
 * 바로 고르러 가는 버튼 하나만 둡니다.
 */
@Composable
private fun IdleHero(onPickApps: () -> Unit) {
    Spacer(Modifier.height(32.dp))
    StatusLine(status = HomeStatus.Idle, apps = emptyList())
    Spacer(Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.home_hero_idle),
        style = SlText.HomeLine,
        color = SlColor.TextPrimary,
    )
    Spacer(Modifier.height(16.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepLockMascot(
            modifier = Modifier.size(width = 48.dp, height = 59.dp),
            mood = MascotMood.Resting,
        )
        Spacer(Modifier.width(8.dp))
        StepiSays(
            text = stringResource(R.string.home_stepi_idle),
            tail = BubbleTail.Start,
            textAlign = TextAlign.Start,
            modifier = Modifier.weight(1f, fill = false),
        )
    }
    Spacer(Modifier.height(24.dp))
    PrimaryButton(text = stringResource(R.string.home_pick_apps), onClick = onPickApps)
}

/**
 * 앱을 잠그면 채워야 할 조건 — 목표만 보여 주고 진행률은 그리지 않습니다.
 * 잠그기 전부터 링이 차오르면 "벌써 진행 중"으로 읽힙니다.
 */
@Composable
private fun ConditionPreview(conditions: List<UnlockCondition>, settings: LockSettings) {
    Column {
        conditions.forEachIndexed { index, condition ->
            if (index > 0) SlDivider()
            ConditionRow(
                title = stringResource(condition.titleRes),
                value = stringResource(R.string.home_condition_goal, condition.goalText(settings)),
                leading = {
                    IconTile(
                        icon = condition.icon(),
                        tint = SlColor.TextSecondary,
                        background = SlColor.SurfaceAlt,
                        size = 40.dp,
                        shape = CircleShape,
                        iconSize = 20.dp,
                    )
                },
            )
        }
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

private fun UnlockCondition.icon(): ImageVector = when (this) {
    UnlockCondition.Steps -> SlIcons.Steps
    UnlockCondition.Sleep -> SlIcons.Moon
    UnlockCondition.Pomodoro -> SlIcons.Timer
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

/** "수면 7시간 채웠어요"의 앞부분. */
@Composable
private fun UnlockCondition.doneText(settings: LockSettings): String = when (this) {
    UnlockCondition.Steps -> stringResource(
        R.string.home_strip_steps,
        settings.stepGoal.formatThousands(),
    )
    UnlockCondition.Sleep -> stringResource(
        R.string.home_strip_sleep,
        sleepGoalLabel(settings.sleepGoalHours),
    )
    UnlockCondition.Pomodoro -> stringResource(R.string.home_strip_pomodoro, settings.pomodoroGoal)
}

@Composable
private fun UnlockCondition.currentText(stat: DailyStat, settings: LockSettings): String =
    when (this) {
        UnlockCondition.Steps -> stringResource(R.string.home_track_walked, stat.steps.formatThousands())
        UnlockCondition.Sleep -> durationLabel(stat.sleepMinutes)
        UnlockCondition.Pomodoro -> stringResource(R.string.unit_sessions, stat.pomodoroSessions)
    }

@Composable
private fun UnlockCondition.goalText(settings: LockSettings): String = when (this) {
    UnlockCondition.Steps -> stringResource(R.string.unit_steps, settings.stepGoal.formatThousands())
    UnlockCondition.Sleep -> sleepGoalLabel(settings.sleepGoalHours)
    UnlockCondition.Pomodoro -> stringResource(R.string.unit_sessions, settings.pomodoroGoal)
}

/** 걸음 목표를 4등분한 이정표 — 8,000보면 2k · 4k · 6k. */
private fun stepTicks(goal: Int): List<String> = (1..3).map { i ->
    val value = goal * i / 4f / 1000f
    if (value % 1f == 0f) "${value.toInt()}k" else "%.1fk".format(Locale.US, value)
}

/** 걸음 폭으로 어림한 거리. "약"은 붙이지 않고 소수 한 자리로만 — 칸이 좁습니다. */
@Composable
private fun distanceText(steps: Int): String =
    stringResource(R.string.home_today_km, "%.1f".format(Locale.KOREA, steps * STRIDE_METERS / 1000f))

/**
 * 걸음 하나의 폭(m). 성인 보통 걸음 0.65~0.78m 의 가운데쯤입니다.
 */
private const val STRIDE_METERS = 0.7f

/** 보통 빠르기로 걸을 때 1분 걸음 수. "약 25분이면 충분해요"를 어림하는 데만 씁니다. */
private const val STEPS_PER_MINUTE = 100.0

private val GLANCE_CELL = 80.dp

/**
 * 홈 맨 위 한 줄이 말하는 상태. 감시 서비스의 판단 순서와 같습니다.
 * [Idle] 은 잠근 앱이 없어 아무것도 막지 않는 상태입니다.
 */
private enum class HomeStatus { Allowed, Focusing, Unlocked, Locked, Idle }

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
            streak = 5,
            warningTitle = null,
            warningDescription = null,
            onWarningClick = {},
            weekly = SampleData.weekly,
            blockedToday = SampleData.blockedToday,
            usageToday = SampleData.usageToday,
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
            streak = 5,
            warningTitle = null,
            warningDescription = null,
            onWarningClick = {},
            weekly = SampleData.weekly,
            blockedToday = SampleData.blockedToday,
            usageToday = SampleData.usageToday,
        )
    }
}

/** 잠근 앱이 없는 첫 상태 — 조건이 진행되지 않고 고르러 가는 길만 보입니다. */
@Preview(widthDp = 412, heightDp = 892, name = "No apps")
@Composable
private fun HomeScreenNoAppsPreview() {
    StepLockTheme {
        HomeScreen(
            userName = null,
            stat = SampleData.today,
            settings = SampleData.settings.copy(blockedAppIds = emptySet()),
            apps = emptyList(),
            selectedTab = NavTab.Home,
            onTabSelected = {},
            onManageLocks = {},
            onPomodoroClick = {},
            streak = 0,
            warningTitle = null,
            warningDescription = null,
            onWarningClick = {},
        )
    }
}
