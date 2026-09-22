package com.steplock.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.steplock.app.R
import com.steplock.app.ads.SlBannerAd
import com.steplock.app.data.DailyStat
import com.steplock.app.data.LockSettings
import com.steplock.app.data.SampleData
import com.steplock.app.ui.components.BottomNavBar
import com.steplock.app.ui.components.ConditionRow
import com.steplock.app.ui.components.IconTile
import com.steplock.app.ui.components.NavTab
import com.steplock.app.ui.components.SectionLabel
import com.steplock.app.ui.components.SlDivider
import com.steplock.app.ui.components.SlEmptyState
import com.steplock.app.ui.components.SlIcons
import com.steplock.app.ui.components.SlPanel
import com.steplock.app.ui.components.SlSegmented
import com.steplock.app.ui.components.WeeklyBarChart
import com.steplock.app.ui.theme.SlColor
import com.steplock.app.ui.theme.SlDimen
import com.steplock.app.ui.theme.SlText
import com.steplock.app.ui.theme.StepLockTheme
import com.steplock.app.ui.util.durationLabel
import com.steplock.app.ui.util.formatThousands
import com.steplock.app.ui.util.sleepGoalMinutes
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 통계. 기간을 7일과 30일로 바꿔 볼 수 있습니다.
 *
 * 30일치는 이미 기기에 있어서(`HISTORY_DAYS`) 한 번에 받아 두고 화면에서
 * 잘라 씁니다 — 기간을 누를 때마다 다시 읽지 않습니다.
 *
 * 조건마다 **평균과 달성률**을 같이 보여 줍니다. 달성 일수만 있으면 기간을 바꿨을 때
 * 숫자가 커진 게 잘해서인지 기간이 길어서인지 구분되지 않습니다.
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
    var periodIndex by remember { mutableIntStateOf(0) }
    val days = if (periodIndex == 0) weekly else monthly

    val sleepGoalMinutes = sleepGoalMinutes(settings.sleepGoalHours)
    val hasRecords = days.any { it.hasAnything }
    // 배너 노출 기준은 30일로 봅니다 — 기간을 누를 때마다 배너가 나타났다
    // 사라지면 그 자체가 방해가 됩니다.
    val hasAnyRecords = monthly.any { it.hasAnything }

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

            // 기간 전환은 빈 상태보다 위에 둡니다. 7일은 비었는데 30일에는
            // 기록이 있을 수 있어서, 빈 화면에서도 기간을 바꿀 수 있어야 합니다.
            Spacer(Modifier.height(16.dp))
            SlSegmented(
                options = listOf(
                    stringResource(R.string.stats_period_week),
                    stringResource(R.string.stats_period_month),
                ),
                selectedIndex = periodIndex,
                onSelect = { periodIndex = it },
                modifier = Modifier.fillMaxWidth(0.55f),
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

            Spacer(Modifier.height(28.dp))
            SectionLabel(stringResource(R.string.stats_section_streak))
            Spacer(Modifier.height(12.dp))
            SlPanel(contentPadding = PaddingValues(SlDimen.PanelPadding)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    StreakFigure(
                        label = stringResource(R.string.stats_streak_current),
                        days = streak,
                        highlight = true,
                        modifier = Modifier.weight(1f),
                    )
                    StreakFigure(
                        label = stringResource(R.string.stats_streak_longest),
                        days = longestStreak,
                        highlight = false,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(28.dp))
            SectionLabel(stringResource(R.string.stats_section_steps))
            Spacer(Modifier.height(12.dp))
            StepsPanel(days = days, goal = settings.stepGoal)

            Spacer(Modifier.height(28.dp))
            SectionLabel(stringResource(R.string.stats_section_conditions))
            Spacer(Modifier.height(12.dp))
            SlPanel {
                RecordRow(
                    icon = SlIcons.Steps,
                    title = stringResource(R.string.condition_steps),
                    enabled = settings.stepsEnabled,
                    average = stringResource(
                        R.string.stats_avg_steps,
                        days.average { it.steps }.formatThousands(),
                    ),
                    achieved = days.count { it.steps >= settings.stepGoal },
                    total = days.size,
                )
                SlDivider()
                RecordRow(
                    icon = SlIcons.Moon,
                    title = stringResource(R.string.condition_sleep),
                    enabled = settings.sleepEnabled,
                    average = stringResource(
                        R.string.stats_avg_sleep,
                        durationLabel(days.average { it.sleepMinutes }),
                    ),
                    achieved = days.count { it.sleepMinutes >= sleepGoalMinutes },
                    total = days.size,
                )
                SlDivider()
                RecordRow(
                    icon = SlIcons.Timer,
                    title = stringResource(R.string.condition_pomodoro),
                    enabled = settings.pomodoroEnabled,
                    average = stringResource(
                        R.string.stats_avg_sessions,
                        days.averageDecimal { it.pomodoroSessions },
                    ),
                    achieved = days.count { it.pomodoroSessions >= settings.pomodoroGoal },
                    total = days.size,
                )
            }
        }

        // 배너는 이 화면에만 둡니다. 통계는 들여다보는 화면이라 광고가 가로막는
        // 작업이 없습니다. 기록이 하나도 없을 때는 붙이지 않습니다 —
        // 처음 켠 사람에게 빈 통계와 광고만 보이면 앱의 첫인상이 광고가 됩니다.
        if (hasAnyRecords) {
            SlBannerAd()
        }

        BottomNavBar(selected = selectedTab, onSelect = onTabSelected)
    }
}

/**
 * 걸음 수 차트와 요약 세 칸.
 *
 * 30일을 고르면 막대가 30개가 되어 요일 라벨을 다 넣을 수 없습니다. 그래서
 * 라벨은 7일일 때만 요일로 모두 적고, 30일일 때는 양 끝과 가운데 날짜만 둡니다.
 */
@Composable
private fun StepsPanel(days: List<DailyStat>, goal: Int) {
    val weekdayFormatter = DateTimeFormatter.ofPattern(
        stringResource(R.string.stats_weekday_pattern),
        Locale.KOREAN,
    )
    val dateFormatter = remember { DateTimeFormatter.ofPattern("M/d", Locale.KOREAN) }
    val achievedDays = days.count { it.steps >= goal }

    SlPanel(contentPadding = PaddingValues(SlDimen.PanelPadding)) {
        Text(
            text = stringResource(
                R.string.stats_today_steps,
                days.lastOrNull()?.steps?.formatThousands() ?: "0",
            ),
            style = SlText.RowTitle,
            color = SlColor.TextPrimary,
        )
        Spacer(Modifier.height(16.dp))
        WeeklyBarChart(
            values = days.map { it.steps },
            goal = goal,
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
        )
        Spacer(Modifier.height(8.dp))
        if (days.size <= 7) {
            Row(modifier = Modifier.fillMaxWidth()) {
                days.forEach { day ->
                    Text(
                        text = day.date.format(weekdayFormatter),
                        style = SlText.LabelSm,
                        color = SlColor.TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                listOf(0, days.size / 2, days.size - 1).forEach { index ->
                    Text(
                        text = days[index].date.format(dateFormatter),
                        style = SlText.LabelSm,
                        color = SlColor.TextSecondary,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        SlDivider()
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            StatFigure(
                label = stringResource(R.string.stats_figure_average),
                value = days.average { it.steps }.formatThousands(),
                modifier = Modifier.weight(1f),
            )
            StatFigure(
                label = stringResource(R.string.stats_figure_best),
                value = (days.maxOfOrNull { it.steps } ?: 0).formatThousands(),
                modifier = Modifier.weight(1f),
            )
            StatFigure(
                label = stringResource(R.string.stats_figure_goal_rate),
                value = stringResource(R.string.stats_percent, percent(achievedDays, days.size)),
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.stats_steps_summary, goal.formatThousands()),
            style = SlText.Caption,
            color = SlColor.TextSecondary,
        )
    }
}

/**
 * 조건 한 줄 — 평균과 달성률.
 *
 * 퍼센트에는 색을 주지 않습니다. 몇 퍼센트부터 "잘한 것"인지는 사람마다 달라서,
 * 색으로 판정하면 앱이 임의로 점수를 매기는 셈이 됩니다.
 */
@Composable
private fun RecordRow(
    icon: ImageVector,
    title: String,
    enabled: Boolean,
    average: String,
    achieved: Int,
    total: Int,
) {
    ConditionRow(
        title = title,
        value = if (enabled) {
            average
        } else {
            stringResource(R.string.stats_condition_off, average)
        },
        leading = {
            IconTile(
                icon = icon,
                // 꺼 둔 조건은 아이콘 색을 낮춰 목록 안에서 구분됩니다.
                tint = if (enabled) SlColor.BrandDeep else SlColor.TextTertiary,
                background = SlColor.SurfaceAlt,
                size = SlDimen.TouchTarget,
                shape = RoundedCornerShape(SlDimen.RadiusField),
                iconSize = 22.dp,
            )
        },
        trailing = {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    // 요약 칸의 숫자와 같은 크기를 씁니다. 더 크게 하면 왼쪽
                    // 평균 글자가 밀려 두 줄로 접힙니다.
                    text = stringResource(R.string.stats_percent, percent(achieved, total)),
                    style = SlText.RowTitle,
                    color = SlColor.TextPrimary,
                )
                Text(
                    text = stringResource(R.string.stats_record_days, total, achieved),
                    style = SlText.LabelSm,
                    color = SlColor.TextSecondary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        },
    )
}

/** 요약 한 칸 — 라벨 위, 값 아래. 세 개를 나란히 두어도 글자가 겹치지 않습니다. */
@Composable
private fun StatFigure(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = SlText.LabelSm, color = SlColor.TextSecondary)
        Text(
            text = value,
            style = SlText.RowTitle,
            color = SlColor.TextPrimary,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/**
 * 라벨과 숫자를 한 줄에 둡니다. 두 줄로 쌓으면 아래 차트와 조건 목록이
 * 화면 밖으로 밀려서, 요약인데 자리를 제일 많이 차지하게 됩니다.
 */
@Composable
private fun StreakFigure(
    label: String,
    days: Int,
    highlight: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = SlText.LabelSm, color = SlColor.TextSecondary)
        Text(
            text = stringResource(R.string.stats_streak_days, days),
            style = SlText.StatusTitle,
            color = if (highlight) SlColor.Brand else SlColor.TextPrimary,
        )
    }
}

private val DailyStat.hasAnything: Boolean
    get() = steps > 0 || sleepMinutes > 0 || pomodoroSessions > 0

private inline fun List<DailyStat>.average(value: (DailyStat) -> Int): Int =
    if (isEmpty()) 0 else sumOf(value) / size

/** 세션은 하루 몇 개 수준이라 정수로 줄이면 0 또는 1만 남습니다. */
private inline fun List<DailyStat>.averageDecimal(value: (DailyStat) -> Int): String =
    if (isEmpty()) "0" else "%.1f".format(Locale.KOREA, sumOf(value).toFloat() / size)

private fun percent(part: Int, total: Int): Int =
    if (total <= 0) 0 else (part * 100) / total

@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun StatsScreenPreview() {
    StepLockTheme {
        StatsScreen(
            weekly = SampleData.weekly,
            monthly = SampleData.monthly,
            settings = SampleData.settings,
            streak = 5,
            longestStreak = 7,
            selectedTab = NavTab.Stats,
            onTabSelected = {},
        )
    }
}
