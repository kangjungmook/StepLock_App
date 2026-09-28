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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.steplock.app.R
import com.steplock.app.data.DailyStat
import com.steplock.app.data.InstalledApp
import com.steplock.app.data.LockSettings
import com.steplock.app.data.SampleData
import com.steplock.app.data.TemporaryAllow
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
import com.steplock.app.ui.util.sleepGoalLabel
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

/**
 * 홈. 위에서 아래로 **지금 → 오늘 → 최근 → 대상** 순서입니다.
 *
 * 1. 지금: 상태 알약과 한 문장("2,760보만 더 걸으면 잠금이 풀려요"), 스텝이가 걷는 트랙.
 *    화면을 열었을 때 가장 알고 싶은 "지금 잠겨 있나, 뭘 하면 되나"를 문장 하나로 답합니다.
 * 2. 나머지 조건(켠 게 둘 이상일 때) — 잠금을 푸는 데 직접 관계된 것이라 바로 아래.
 * 3. 오늘: 걸은 거리·막은 횟수·남은 임시 허용. 걸음 조건 하나만 켠 기본 상태에서도
 *    화면이 비지 않도록, 꾸밈이 아니라 **이 앱만 알려 줄 수 있는 숫자**로 채웁니다.
 * 4. 최근 7일: 요일마다 링. 채운 날은 꽉 찬 원, 못 채운 날은 얼마나 갔는지.
 * 5. 차단 중인 앱: 앱마다 오늘 몇 번 막았는지. 많이 막은 앱이 위로 옵니다.
 *
 * 카드는 "나머지 조건" 하나뿐입니다. 나머지 구역은 여백과 섹션 제목으로만 나눠
 * 같은 크기의 상자가 쌓이지 않게 합니다. 모양은 원(링·알약·아이콘) 위주입니다.
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
) {
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
            HomeHeader(userName = userName, stat = stat, streak = streak)

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

                // 1. 지금 — 결론을 문장으로 먼저 말하고, 트랙이 근거를 보여 줍니다.
                Spacer(Modifier.height(32.dp))
                StatusPill(status)
                Spacer(Modifier.height(12.dp))
                HeroHeadline(
                    status = status,
                    hero = hero,
                    stat = stat,
                    settings = settings,
                    allowUntil = temporaryAllowUntil,
                )
                Spacer(Modifier.height(16.dp))
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

                // 2. 나머지 조건 — 트랙이 첫 조건을 보여 주므로 여기엔 나머지만.
                val rest = conditions.filter { it != hero }
                if (rest.isNotEmpty()) {
                    HomeSectionHeader(stringResource(R.string.home_section_conditions_rest))
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

                // 3. 오늘
                HomeSectionHeader(stringResource(R.string.home_section_today))
                TodayFigures(
                    steps = stat.steps,
                    blockedTotal = blockedToday.values.sum(),
                    allowRemaining = temporaryAllowRemaining,
                )

                // 4. 최근 7일
                if (weekly.isNotEmpty()) {
                    val achievedDays = weekly.count { UnlockEvaluator.isUnlocked(settings, it) }
                    val openStats = { onTabSelected(NavTab.Stats) }
                    HomeSectionHeader(
                        title = stringResource(R.string.home_section_week),
                        trailing = {
                            WeekSummaryLink(
                                text = stringResource(
                                    R.string.home_week_summary,
                                    weekly.size,
                                    achievedDays,
                                ),
                                onClick = openStats,
                            )
                        },
                    )
                    WeekRings(
                        days = weekly,
                        settings = settings,
                        hero = hero,
                        onClick = openStats,
                    )
                }
            }

            // 5. 차단 중인 앱
            HomeSectionHeader(
                title = stringResource(R.string.home_section_blocked_apps),
                trailing = if (apps.isEmpty()) {
                    null
                } else {
                    {
                        // 감지는 앱별 상태가 아니라 하나뿐인 감시 서비스의 상태입니다.
                        if (warningTitle == null) DetectingStatus()
                        // 목록 전체를 한 곳에서 고칩니다. 줄마다 화살표를 달면 앱마다 다른
                        // 화면이 있는 것처럼 읽히는데, 실제로는 모두 같은 관리 화면입니다.
                        TextLink(
                            text = stringResource(R.string.home_manage_apps),
                            onClick = onManageLocks,
                            style = SlText.LinkSm,
                            color = SlColor.BrandInk,
                            underline = false,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                },
            )
            if (apps.isEmpty()) {
                SlPanel {
                    SlEmptyState(
                        title = stringResource(R.string.home_no_apps_title),
                        description = stringResource(R.string.home_no_apps_desc),
                        onClick = onManageLocks,
                    )
                }
            } else {
                // 오늘 많이 막은 앱이 위로 — 가장 손이 많이 가는 앱이 한눈에 보입니다.
                // 같은 횟수끼리는 원래 순서(이름 순)를 지킵니다.
                apps.sortedByDescending { blockedToday[it.packageName] ?: 0 }
                    .forEachIndexed { index, app ->
                        if (index > 0) SlDivider()
                        BlockedAppRow(app = app, blockedCount = blockedToday[app.packageName] ?: 0)
                    }
            }
        }

        BottomNavBar(selected = selectedTab, onSelect = onTabSelected)
    }
}

/** 인사와 날짜, 오른쪽에 연속 달성. 연속 기록은 "나"에 관한 숫자라 인사 옆에 둡니다. */
@Composable
private fun HomeHeader(userName: String?, stat: DailyStat, streak: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (userName != null) {
                    stringResource(R.string.home_greeting, stringResource(greetingRes()), userName)
                } else {
                    stringResource(greetingRes())
                },
                style = SlText.Greeting,
                color = SlColor.TextPrimary,
            )
            Spacer(Modifier.height(4.dp))
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
        }
        // 하루치로는 자랑할 게 없어서 이틀 이상일 때만 보여 줍니다.
        if (streak >= 2) StreakBadge(streak, modifier = Modifier.padding(top = 4.dp))
    }
}

/** 섹션 제목 줄. 섹션 사이는 카드 대신 이 여백(32)으로 나눕니다. */
@Composable
private fun HomeSectionHeader(
    title: String,
    trailing: (@Composable () -> Unit)? = null,
) {
    Spacer(Modifier.height(32.dp))
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

/** 상태를 색 하나로 먼저 알립니다. 잠김은 앰버, 열림·허용·집중은 브랜드. */
@Composable
private fun StatusPill(status: HomeStatus) {
    val locked = status == HomeStatus.Locked
    val container = if (locked) SlColor.AmberSurface else SlColor.BrandTintAlt
    val dot = if (locked) SlColor.Amber else SlColor.Brand
    val content = if (locked) SlColor.AmberText else SlColor.BrandDeep
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(container)
            .height(28.dp)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dot),
        )
        Text(
            text = stringResource(
                when (status) {
                    HomeStatus.Locked -> R.string.home_pill_locked
                    HomeStatus.Unlocked -> R.string.home_pill_unlocked
                    HomeStatus.Allowed -> R.string.home_pill_allowed
                    HomeStatus.Focusing -> R.string.home_pill_focusing
                },
            ),
            style = SlText.Chip,
            color = content,
        )
    }
}

/**
 * 홈의 한 문장. 할 일의 숫자(2,760보·2번·오후 3:05)만 브랜드 색으로 띄워,
 * 문장을 다 읽지 않아도 무엇을 얼마나 하면 되는지 먼저 보이게 합니다.
 */
@Composable
private fun HeroHeadline(
    status: HomeStatus,
    hero: UnlockCondition,
    stat: DailyStat,
    settings: LockSettings,
    allowUntil: Long?,
) {
    val highlight: String?
    val text: String
    when (status) {
        HomeStatus.Locked -> when {
            // 전부 만족 모드에서 맨 앞 조건은 채웠지만 다른 조건이 남은 경우.
            hero.isAchieved(stat, settings) -> {
                highlight = null
                text = stringResource(R.string.home_hero_rest)
            }
            hero == UnlockCondition.Steps -> {
                highlight = stringResource(
                    R.string.unit_steps,
                    (settings.stepGoal - stat.steps).coerceAtLeast(0).formatThousands(),
                )
                text = stringResource(R.string.home_hero_steps, highlight)
            }
            // 지금 당장 채울 수 없는 조건이라 남은 시간을 숫자로 재촉하지 않습니다.
            hero == UnlockCondition.Sleep -> {
                highlight = null
                text = stringResource(R.string.home_hero_sleep)
            }
            else -> {
                highlight = stringResource(
                    R.string.home_times,
                    (settings.pomodoroGoal - stat.pomodoroSessions).coerceAtLeast(1),
                )
                text = stringResource(R.string.home_hero_pomodoro, highlight)
            }
        }
        HomeStatus.Unlocked -> {
            highlight = null
            text = stringResource(R.string.home_hero_unlocked)
        }
        HomeStatus.Allowed -> {
            highlight = clockText(allowUntil ?: 0L)
            text = stringResource(R.string.home_hero_allowed, highlight)
        }
        HomeStatus.Focusing -> {
            highlight = null
            text = stringResource(R.string.home_hero_focusing)
        }
    }
    Text(
        text = text.highlighted(highlight, SpanStyle(color = SlColor.BrandInk)),
        style = SlText.HeroHeadline,
        color = SlColor.TextPrimary,
    )
}

/**
 * 오늘의 숫자 셋 — 걸은 거리, 막은 횟수, 남은 임시 허용.
 *
 * 카드로 감싸지 않고 세로 구분선으로만 나눕니다. 위의 트랙이 이미 걸음 수를
 * 보여 주므로 여기서는 걸음을 되풀이하지 않고 거리로 바꿔 말합니다.
 */
@Composable
private fun TodayFigures(steps: Int, blockedTotal: Int, allowRemaining: Int) {
    val km = steps * STRIDE_METERS / 1000f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Figure(
            // 걸음 폭으로 어림한 값이라 "약"을 붙이되, 숫자보다 작게 둡니다.
            value = buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = SlColor.TextSecondary,
                    ),
                ) {
                    append(stringResource(R.string.home_today_about))
                    append(" ")
                }
                append(stringResource(R.string.home_today_km, "%.1f".format(Locale.KOREA, km)))
            },
            label = stringResource(R.string.home_today_distance),
            modifier = Modifier.weight(1f),
        )
        FigureDivider()
        Figure(
            value = AnnotatedString(stringResource(R.string.home_times, blockedTotal)),
            label = stringResource(R.string.home_today_blocked),
            modifier = Modifier.weight(1f),
        )
        FigureDivider()
        Figure(
            value = AnnotatedString(stringResource(R.string.home_times, allowRemaining)),
            label = stringResource(R.string.home_today_allow_left),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun Figure(value: AnnotatedString, label: String, modifier: Modifier = Modifier) {
    Column(
        // 화면 읽기에서는 "걸은 거리, 약 3.7km" 처럼 한 덩어리로 읽힙니다.
        modifier = modifier.semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = value, style = SlText.FigureValue, color = SlColor.TextPrimary)
        Spacer(Modifier.height(4.dp))
        Text(text = label, style = SlText.LabelSm, color = SlColor.TextSecondary)
    }
}

@Composable
private fun FigureDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(32.dp)
            .background(SlColor.Border),
    )
}

/** "7일 중 5일 달성 ›" — 누르면 통계로 갑니다. 44dp 터치 영역을 지킵니다. */
@Composable
private fun WeekSummaryLink(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(SlDimen.RadiusSmall))
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = SlDimen.TouchTarget)
            .padding(start = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = text, style = SlText.LinkSm, color = SlColor.BrandInk)
        SlChevron(tint = SlColor.BrandInk, modifier = Modifier.padding(start = 2.dp))
    }
}

/**
 * 최근 7일을 요일마다 링 하나로. 채운 날은 꽉 찬 원에 체크, 못 채운 날은
 * 맨 앞 조건을 얼마나 채웠는지 호로 보여 줍니다 — "못 했다"보다 "여기까지 갔다".
 *
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
                    ProgressRing(
                        progress = progress,
                        size = 40.dp,
                        radius = 18.dp,
                        strokeWidth = 4.dp,
                    )
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

/** 차단 중인 앱 한 줄 — 아이콘, 이름, 오늘 막은 횟수. */
@Composable
private fun BlockedAppRow(app: InstalledApp, blockedCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 기기에서 읽은 실제 아이콘. 첫 글자 뱃지로는 "라이트" 같은
        // 변종을 구분할 수 없습니다.
        AppIcon(packageName = app.packageName, label = app.label, size = 40.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = app.label, style = SlText.ListItem, color = SlColor.TextPrimary)
            Spacer(Modifier.height(2.dp))
            if (blockedCount > 0) {
                val times = stringResource(R.string.home_times, blockedCount)
                Text(
                    text = stringResource(R.string.home_app_blocked_today, times).highlighted(
                        times,
                        SpanStyle(color = SlColor.AmberText, fontWeight = FontWeight.Bold),
                    ),
                    style = SlText.LabelSm,
                    color = SlColor.TextSecondary,
                )
            } else {
                Text(
                    text = stringResource(R.string.home_app_blocked_none),
                    style = SlText.LabelSm,
                    color = SlColor.TextTertiary,
                )
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

/** 연속 달성 배지. 숫자 하나로만 자랑합니다. */
@Composable
private fun StreakBadge(streak: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(SlColor.BrandTintAlt)
            .padding(horizontal = 10.dp, vertical = 6.dp),
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

/** 문장 안의 [part] 한 군데에만 [style] 을 입힙니다. 없거나 못 찾으면 그대로. */
private fun String.highlighted(part: String?, style: SpanStyle): AnnotatedString {
    val start = if (part.isNullOrEmpty()) -1 else indexOf(part)
    if (start < 0) return AnnotatedString(this)
    return buildAnnotatedString {
        append(this@highlighted)
        addStyle(style, start, start + part!!.length)
    }
}

/**
 * 걸음 하나의 폭(m). 성인 보통 걸음 0.65~0.78m 의 가운데쯤입니다. 거리는 이 값으로
 * 어림하므로 화면에 "약"을 붙입니다.
 */
private const val STRIDE_METERS = 0.7f

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
        )
    }
}

/**
 * 기본 설정(걸음 하나만, 잠근 앱 둘) — 홈이 가장 비기 쉬운 상태에서도 채워 보이는지 봅니다.
 */
@Preview(widthDp = 412, heightDp = 892, name = "Steps only")
@Composable
private fun HomeScreenStepsOnlyPreview() {
    StepLockTheme {
        HomeScreen(
            userName = null,
            stat = SampleData.today,
            settings = SampleData.settings.copy(
                sleepEnabled = false,
                pomodoroEnabled = false,
                requireAllConditions = false,
            ),
            apps = SampleData.apps.take(2),
            selectedTab = NavTab.Home,
            onTabSelected = {},
            onManageLocks = {},
            onPomodoroClick = {},
            streak = 0,
            warningTitle = null,
            warningDescription = null,
            onWarningClick = {},
            weekly = SampleData.weekly,
            blockedToday = SampleData.blockedToday,
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
