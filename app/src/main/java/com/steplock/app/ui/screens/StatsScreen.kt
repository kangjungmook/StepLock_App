package com.steplock.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.steplock.app.R
import com.steplock.app.ads.SlBannerAd
import com.steplock.app.data.DailyStat
import com.steplock.app.data.LockSettings
import com.steplock.app.data.SampleData
import com.steplock.app.data.UnlockEvaluator
import com.steplock.app.ui.components.BottomNavBar
import com.steplock.app.ui.components.NavTab
import com.steplock.app.ui.components.SlDivider
import com.steplock.app.ui.components.SlEmptyState
import com.steplock.app.ui.components.SlPanel
import com.steplock.app.ui.theme.SlColor
import com.steplock.app.ui.theme.SlDimen
import com.steplock.app.ui.theme.SlText
import com.steplock.app.ui.theme.StepLockTheme
import com.steplock.app.ui.util.durationLabel
import com.steplock.app.ui.util.formatThousands
import com.steplock.app.ui.util.minutesLabel
import com.steplock.app.ui.util.numeralText
import com.steplock.app.ui.util.primaryCondition
import com.steplock.app.ui.util.progress
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * 통계 — **달력 한 장**.
 *
 * 차트와 요약 칸을 쌓던 화면을 지난 30일 달력 하나로 바꿨습니다. 칸 하나가 하루이고,
 * 채운 날은 흑연, 못 채운 날은 얼마나 갔는지만큼 옅은 회색, 기록이 없는 날은 테두리만,
 * 고른 날은 주황입니다. "얼마나 꾸준했나"는 검은 칸이 이어진 모양으로 보이고,
 * 하루를 누르면 그날의 걸음 · 잠근 앱 사용 · 잠겨 있던 시간이 아래에 나옵니다.
 *
 * 맨 위는 연속 기록 숫자 하나, 맨 아래는 하루 평균 몇 줄 — 그 사이는 달력뿐입니다.
 */
@Composable
fun StatsScreen(
    weekly: List<DailyStat>,
    monthly: List<DailyStat>,
    settings: LockSettings,
    streak: Int,
    longestStreak: Int,
    selectedTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val days = monthly.ifEmpty { weekly }
    val hasRecords = days.any { it.hasAnything }
    var selectedIndex by rememberSaveable(days.size) { mutableIntStateOf(days.lastIndex) }
    val selected = days.getOrNull(selectedIndex)

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

            if (!hasRecords) {
                Spacer(Modifier.height(24.dp))
                SlPanel {
                    SlEmptyState(
                        title = stringResource(R.string.stats_empty_title),
                        description = stringResource(R.string.stats_empty_desc),
                    )
                }
                return@Column
            }

            Spacer(Modifier.height(20.dp))
            StreakHero(streak = streak, longest = longestStreak)

            Spacer(Modifier.height(28.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.stats_calendar_title, days.size),
                    style = SlText.SectionTitle,
                    color = SlColor.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                LegendDot(color = SlColor.TextPrimary, label = stringResource(R.string.stats_legend_done))
                Spacer(Modifier.width(12.dp))
                LegendDot(color = SlColor.TrackOff, label = stringResource(R.string.stats_legend_not))
            }
            Spacer(Modifier.height(12.dp))
            CalendarGrid(
                days = days,
                settings = settings,
                selectedIndex = selectedIndex,
                onSelect = { selectedIndex = it },
            )

            if (selected != null) {
                Spacer(Modifier.height(24.dp))
                SlDivider()
                Spacer(Modifier.height(20.dp))
                DayDetail(day = selected, isToday = selectedIndex == days.lastIndex)
            }

            Spacer(Modifier.height(32.dp))
            Text(
                text = stringResource(R.string.stats_average_title, days.size),
                style = SlText.SectionTitle,
                color = SlColor.TextPrimary,
            )
            Spacer(Modifier.height(4.dp))
            val averages = buildList {
                if (settings.stepsEnabled) {
                    add(
                        stringResource(R.string.condition_steps) to
                            stringResource(R.string.unit_steps, days.average { it.steps }.formatThousands()),
                    )
                }
                add(stringResource(R.string.stats_row_usage) to minutesLabel(days.average { it.blockedUsageMinutes }))
                add(stringResource(R.string.stats_row_locked) to minutesLabel(days.average { it.lockedMinutes }))
                if (settings.sleepEnabled) {
                    add(stringResource(R.string.condition_sleep) to durationLabel(days.average { it.sleepMinutes }))
                }
                if (settings.pomodoroEnabled) {
                    add(
                        stringResource(R.string.condition_pomodoro) to
                            stringResource(R.string.unit_sessions, days.average { it.pomodoroSessions }),
                    )
                }
            }
            averages.forEachIndexed { index, (label, value) ->
                if (index > 0) SlDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .semantics(mergeDescendants = true) {},
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = label,
                        style = SlText.ListItem,
                        color = SlColor.TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = numeralText(value, unitSize = 13.sp),
                        style = SlText.TrailProgress,
                        color = SlColor.TextPrimary,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.stats_usage_note),
                style = SlText.Caption,
                color = SlColor.TextTertiary,
            )
        }

        // 배너는 이 화면에만 둡니다. 기록이 하나도 없을 때는 붙이지 않습니다 —
        // 처음 켠 사람에게 빈 통계와 광고만 보이면 앱의 첫인상이 광고가 됩니다.
        if (monthly.any { it.hasAnything }) {
            SlBannerAd()
        }

        BottomNavBar(selected = selectedTab, onSelect = onTabSelected)
    }
}

/** 연속 기록 — 큰 숫자 하나와 한 줄. */
@Composable
private fun StreakHero(streak: Int, longest: Int) {
    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.semantics(mergeDescendants = true) {},
    ) {
        Text(
            text = streak.toString(),
            style = SlText.HomeNumeral.copy(fontSize = 72.sp, lineHeight = 72.sp),
            color = SlColor.TextPrimary,
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.padding(bottom = 8.dp)) {
            Text(
                text = stringResource(
                    if (streak > 0) R.string.stats_streak_days else R.string.stats_streak_zero_short,
                ),
                style = SlText.HomeLine.copy(fontSize = 18.sp, lineHeight = 24.sp),
                color = SlColor.TextPrimary,
            )
            Text(
                text = if (streak > 0 && streak >= longest) {
                    stringResource(R.string.stats_streak_is_best)
                } else {
                    stringResource(R.string.stats_streak_best, longest)
                },
                style = SlText.RowValue,
                color = SlColor.TextSecondary,
            )
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color),
        )
        Spacer(Modifier.width(4.dp))
        Text(text = label, style = SlText.LabelSm, color = SlColor.TextSecondary)
    }
}

/**
 * 지난 날들을 요일에 맞춰 7칸씩. 첫 줄은 첫날의 요일만큼 비워 둡니다.
 * 칸 하나가 44dp 이상이라 손가락으로 고르기 좋습니다.
 */
@Composable
private fun CalendarGrid(
    days: List<DailyStat>,
    settings: LockSettings,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    val hero = primaryCondition(settings)
    // 일요일부터 시작하는 달력 — 한국 달력의 흔한 순서입니다.
    val lead = days.firstOrNull()?.date?.dayOfWeek?.let { it.value % 7 } ?: 0
    val cells: List<Int?> = List(lead) { null } + days.indices.toList()
    val weekdays = listOf(DayOfWeek.SUNDAY) + DayOfWeek.entries.filter { it != DayOfWeek.SUNDAY }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            weekdays.forEach { day ->
                Text(
                    text = day.getDisplayName(TextStyle.SHORT, Locale.KOREAN),
                    style = SlText.LabelSm,
                    color = SlColor.TextTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        cells.chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                week.forEach { index ->
                    if (index == null) {
                        Spacer(Modifier.weight(1f))
                    } else {
                        DayCell(
                            day = days[index],
                            done = UnlockEvaluator.isUnlocked(settings, days[index]),
                            progress = hero.progress(days[index], settings),
                            selected = index == selectedIndex,
                            onClick = { onSelect(index) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: DailyStat,
    done: Boolean,
    progress: Float,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(SlDimen.RadiusSmall)
    val empty = !day.hasAnything
    // 못 채운 날은 간 만큼 진해집니다 — 거의 채운 날과 아예 안 걸은 날이 다르게 보입니다.
    val partial = lerp(SlColor.TrackOff, SlColor.TextSecondary, (progress * 0.6f).coerceIn(0f, 0.6f))
    val (container, content) = when {
        selected -> SlColor.Brand to SlColor.OnBrand
        done -> SlColor.TextPrimary to SlColor.Background
        empty -> Color.Transparent to SlColor.TextTertiary
        else -> partial to SlColor.TextPrimary
    }
    val label = day.date.format(DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN))
    val state = stringResource(
        when {
            done -> R.string.stats_day_done
            empty -> R.string.stats_day_empty
            else -> R.string.stats_day_not
        },
    )
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(container)
            .then(if (empty && !selected) Modifier.border(1.5.dp, SlColor.BorderStrong, shape) else Modifier)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .semantics { contentDescription = "$label, $state" }
            .padding(horizontal = 6.dp, vertical = 4.dp),
    ) {
        Text(
            text = day.date.dayOfMonth.toString(),
            style = SlText.TrailTick,
            color = content,
        )
    }
}

/** 고른 날의 숫자 셋. */
@Composable
private fun DayDetail(day: DailyStat, isToday: Boolean) {
    val title = day.date.format(DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN))
    Text(
        text = if (isToday) stringResource(R.string.stats_day_today, title) else title,
        style = SlText.RowTitle,
        color = SlColor.TextPrimary,
    )
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(
            stringResource(R.string.unit_steps, day.steps.formatThousands()) to stringResource(R.string.condition_steps),
            minutesLabel(day.blockedUsageMinutes) to stringResource(R.string.stats_row_usage),
            minutesLabel(day.lockedMinutes) to stringResource(R.string.stats_row_locked),
        ).forEach { (value, label) ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .semantics(mergeDescendants = true) {},
            ) {
                Text(
                    text = numeralText(value, unitSize = 13.sp),
                    style = SlText.GlanceValue.copy(fontSize = 28.sp, lineHeight = 30.sp),
                    color = SlColor.TextPrimary,
                    maxLines = 1,
                )
                Spacer(Modifier.height(4.dp))
                Text(text = label, style = SlText.LabelSm, color = SlColor.TextSecondary)
            }
        }
    }
}

private val DailyStat.hasAnything: Boolean
    get() = steps > 0 || sleepMinutes > 0 || pomodoroSessions > 0 ||
        blockedUsageMinutes > 0 || lockedMinutes > 0

private inline fun List<DailyStat>.average(value: (DailyStat) -> Int): Int =
    if (isEmpty()) 0 else sumOf(value) / size

@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun StatsScreenPreview() {
    StepLockTheme {
        StatsScreen(
            weekly = SampleData.weekly,
            monthly = SampleData.monthly,
            settings = SampleData.settings,
            streak = 5,
            longestStreak = 12,
            selectedTab = NavTab.Stats,
            onTabSelected = {},
        )
    }
}

@Preview(widthDp = 412, heightDp = 892, name = "Dark")
@Composable
private fun StatsScreenPreviewDark() {
    StepLockTheme(darkTheme = true) {
        StatsScreen(
            weekly = SampleData.weekly,
            monthly = SampleData.monthly,
            settings = SampleData.settings,
            streak = 5,
            longestStreak = 12,
            selectedTab = NavTab.Stats,
            onTabSelected = {},
        )
    }
}
