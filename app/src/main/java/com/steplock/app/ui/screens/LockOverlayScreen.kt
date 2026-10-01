package com.steplock.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.steplock.app.R
import com.steplock.app.data.DailyStat
import com.steplock.app.data.LockSettings
import com.steplock.app.data.SampleData
import com.steplock.app.data.TemporaryAllow
import com.steplock.app.ui.components.ChipState
import com.steplock.app.ui.components.MascotMood
import com.steplock.app.ui.components.MinuteGrid
import com.steplock.app.ui.components.PrimaryButton
import com.steplock.app.ui.components.SlIcons
import com.steplock.app.ui.components.StatusChip
import com.steplock.app.ui.components.TextLink
import com.steplock.app.ui.components.TrailMap
import com.steplock.app.ui.theme.DarkPalette
import com.steplock.app.ui.theme.LocalSlPalette
import com.steplock.app.ui.theme.SlColor
import com.steplock.app.ui.theme.SlDimen
import com.steplock.app.ui.theme.SlText
import com.steplock.app.ui.util.UnlockCondition
import com.steplock.app.ui.util.enabledConditions
import com.steplock.app.ui.util.formatCountdown
import com.steplock.app.ui.util.formatThousands
import com.steplock.app.ui.util.isAchieved
import com.steplock.app.ui.util.minutesLabel
import com.steplock.app.ui.util.primaryCondition
import com.steplock.app.ui.util.progress
import com.steplock.app.ui.util.sleepGoalLabel
import kotlinx.coroutines.delay

/**
 * 차단한 앱을 열면 덮이는 전체 화면 — **홈과 같은 등산로를 어둡게**.
 *
 * 링 하나와 칩을 가운데 늘어놓던 화면을, 홈의 등산로 그대로 옮겨 왔습니다. 막힌 순간에
 * "지금 길의 어디쯤인가"가 홈과 똑같은 그림으로 보여서, 잠금 화면이 벌이 아니라
 * 같은 길의 한 장면으로 읽힙니다. 맨 위는 막힌 앱 이름, 길 왼쪽에 "얼마나 더",
 * 맨 아래에 할 수 있는 일(닫기 · 5분 허용)만 둡니다.
 *
 * 집중 중이면 길 대신 남은 시간과 25분 칸(한 칸 1분)이 그 자리를 차지합니다.
 *
 * 앱의 화면 모드와 상관없이 **언제나 어두운** 화면입니다 — 앱과 구분돼야 해서,
 * 이 화면 안에서는 다크 팔레트를 내려 줍니다.
 */
@Composable
fun LockOverlayScreen(
    appName: String,
    stat: DailyStat,
    settings: LockSettings,
    temporaryAllowRemaining: Int,
    onDismiss: () -> Unit,
    onTemporaryAllow: () -> Unit,
    modifier: Modifier = Modifier,
    /** 광고를 봐서 임시 허용을 더 받을 수 있는 남은 횟수. */
    adBonusRemaining: Int = 0,
    /**
     * 광고를 보고 임시 허용을 한 번 더 받는 선택지. 받아 둔 광고가 없으면 null 이고,
     * 그럴 때 이 줄은 나타나지 않습니다 — 눌러도 안 되는 줄을 남기지 않습니다.
     */
    onWatchAdForBonus: (() -> Unit)? = null,
    /**
     * 집중 세션이 끝나는 시각. null 이 아니면 집중 중이라 잠긴 것이고, 길 대신 세션의
     * 남은 시간을 그립니다. 세션은 잠금이 걸리는 순간 저절로 시작됩니다.
     */
    focusEndsAt: Long? = null,
    /** 집중 타이머 화면으로 가는 길. 집중 중일 때만 보입니다. */
    onOpenFocus: (() -> Unit)? = null,
    /** 오늘 이 앱을 쓴 시간(분). 사용 기록을 못 읽었으면 null. */
    usedTodayMinutes: Int? = null,
) {
    CompositionLocalProvider(LocalSlPalette provides DarkPalette) {
        LockContent(
            appName = appName,
            stat = stat,
            settings = settings,
            temporaryAllowRemaining = temporaryAllowRemaining,
            onDismiss = onDismiss,
            onTemporaryAllow = onTemporaryAllow,
            modifier = modifier,
            adBonusRemaining = adBonusRemaining,
            onWatchAdForBonus = onWatchAdForBonus,
            focusEndsAt = focusEndsAt,
            onOpenFocus = onOpenFocus,
            usedTodayMinutes = usedTodayMinutes,
        )
    }
}

@Composable
private fun LockContent(
    appName: String,
    stat: DailyStat,
    settings: LockSettings,
    temporaryAllowRemaining: Int,
    onDismiss: () -> Unit,
    onTemporaryAllow: () -> Unit,
    modifier: Modifier,
    adBonusRemaining: Int,
    onWatchAdForBonus: (() -> Unit)?,
    focusEndsAt: Long?,
    onOpenFocus: (() -> Unit)?,
    usedTodayMinutes: Int?,
) {
    val focusing = focusEndsAt != null
    val focusRemaining = rememberRemaining(focusEndsAt)
    val hero = primaryCondition(settings)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SlColor.Background)
            .safeDrawingPadding()
            .padding(horizontal = SlDimen.ScreenPadding, vertical = 16.dp),
    ) {
        // 1. 무엇이 막혔는지 — 앱 이름이 제목입니다.
        Spacer(Modifier.height(12.dp))
        Text(
            text = appName,
            style = SlText.HomeBig,
            color = SlColor.TextPrimary,
            maxLines = 1,
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = SlIcons.PasswordLock,
                contentDescription = null,
                tint = SlColor.BrandInk,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (usedTodayMinutes != null && usedTodayMinutes > 0) {
                    stringResource(R.string.lock_sub_used, minutesLabel(usedTodayMinutes))
                } else {
                    stringResource(R.string.lock_sub)
                },
                style = SlText.Remaining,
                color = SlColor.TextSecondary,
            )
        }

        // 2. 지금 어디쯤인지 — 길(또는 집중 중이면 남은 시간).
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 12.dp),
        ) {
            if (focusing) {
                FocusLockBody(remainingMs = focusRemaining)
            } else {
                TrailMap(
                    progress = hero.progress(stat, settings),
                    ticks = if (hero == UnlockCondition.Steps) lockTicks(settings.stepGoal) else emptyList(),
                    mood = if (hero.progress(stat, settings) >= 1f) MascotMood.Resting else MascotMood.Walking,
                    modifier = Modifier.fillMaxSize(),
                )
                LockHeadline(
                    hero = hero,
                    stat = stat,
                    settings = settings,
                    modifier = Modifier
                        .fillMaxWidth(0.62f)
                        .padding(top = 16.dp),
                )
            }
        }

        // 3. 다른 조건 — 길로 보여 준 조건은 빼고 칩으로.
        if (!focusing) {
            OtherConditions(hero = hero, stat = stat, settings = settings)
        }

        // 4. 할 수 있는 일. 집중 중이면 타이머로 가는 길이 맨 위입니다.
        Spacer(Modifier.height(16.dp))
        if (onOpenFocus != null && focusing) {
            PrimaryButton(text = stringResource(R.string.lock_open_focus), onClick = onOpenFocus)
            Spacer(Modifier.height(8.dp))
            PrimaryButton(
                text = stringResource(R.string.lock_dismiss),
                onClick = onDismiss,
                containerColor = SlColor.SurfaceAlt,
                contentColor = SlColor.TextPrimary,
            )
        } else {
            // 잠금 화면의 주 동작은 "닫기"입니다 — 앱을 떠나 길로 돌아가는 쪽이 이 화면의 뜻.
            PrimaryButton(text = stringResource(R.string.lock_dismiss), onClick = onDismiss)
        }
        // 한도를 다 쓰면 링크 자체를 없앱니다. 광고 줄은 기본 한도를 다 쓴 뒤에만.
        // 집중 중에는 임시 허용이 없습니다 — 세션은 멈출 수 없습니다.
        when {
            focusing -> FootNote(stringResource(R.string.lock_focus_no_allow))

            temporaryAllowRemaining > 0 -> TextLink(
                text = stringResource(
                    R.string.lock_temporary_allow,
                    TemporaryAllow.MINUTES,
                    temporaryAllowRemaining,
                ),
                onClick = onTemporaryAllow,
                modifier = Modifier.fillMaxWidth(),
                style = SlText.LinkSm,
                color = SlColor.TextSecondary,
            )

            onWatchAdForBonus != null && adBonusRemaining > 0 -> TextLink(
                text = stringResource(
                    R.string.lock_temporary_allow_ad,
                    TemporaryAllow.MINUTES,
                    adBonusRemaining,
                ),
                onClick = onWatchAdForBonus,
                modifier = Modifier.fillMaxWidth(),
                style = SlText.LinkSm,
                color = SlColor.TextSecondary,
            )

            else -> FootNote(stringResource(R.string.lock_temporary_allow_exhausted))
        }
    }
}

/** 길 왼쪽의 "얼마나 더" — 홈과 같은 표지판 숫자. */
@Composable
private fun LockHeadline(
    hero: UnlockCondition,
    stat: DailyStat,
    settings: LockSettings,
    modifier: Modifier = Modifier,
) {
    var number: String? = null
    var unit: String? = null
    var word: String? = null
    val line: String
    when {
        hero.isAchieved(stat, settings) -> {
            word = stringResource(R.string.home_big_almost)
            line = stringResource(R.string.lock_remaining_done)
        }
        hero == UnlockCondition.Steps -> {
            number = (settings.stepGoal - stat.steps).coerceAtLeast(0).formatThousands()
            unit = stringResource(R.string.home_unit_steps)
            line = stringResource(R.string.lock_line_steps)
        }
        hero == UnlockCondition.Sleep -> {
            word = stringResource(R.string.home_big_sleep)
            line = stringResource(R.string.lock_line_sleep)
        }
        else -> {
            number = (settings.pomodoroGoal - stat.pomodoroSessions).coerceAtLeast(1).toString()
            unit = stringResource(R.string.home_unit_times)
            line = stringResource(R.string.lock_line_pomodoro)
        }
    }
    Column(modifier = modifier.semantics(mergeDescendants = true) {}) {
        if (number != null) {
            Text(
                text = buildAnnotatedString {
                    append(number)
                    if (unit != null) withStyle(SlText.HomeUnit.toSpanStyle()) { append(" $unit") }
                },
                style = SlText.HomeNumeral.copy(fontSize = 76.sp, lineHeight = 76.sp),
                color = SlColor.TextPrimary,
            )
        } else if (word != null) {
            Text(text = word, style = SlText.HomeBig.copy(fontSize = 32.sp), color = SlColor.TextPrimary)
        }
        Spacer(Modifier.height(6.dp))
        Text(text = line, style = SlText.HomeLine.copy(fontSize = 19.sp), color = SlColor.TextPrimary)
    }
}

/** 집중 중 — 큰 남은 시간과 25분 칸. */
@Composable
private fun FocusLockBody(remainingMs: Long) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text(
            text = formatCountdown(remainingMs),
            style = SlText.HomeNumeral.copy(fontSize = 112.sp, lineHeight = 112.sp),
            color = SlColor.TextPrimary,
        )
        Text(
            text = stringResource(R.string.lock_focusing),
            style = SlText.HomeLine.copy(fontSize = 18.sp, lineHeight = 26.sp),
            color = SlColor.TextPrimary,
        )
        Spacer(Modifier.height(24.dp))
        MinuteGrid(
            remainingMs = remainingMs,
            description = stringResource(R.string.pomodoro_grid_desc, formatCountdown(remainingMs)),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.pomodoro_grid_caption),
            style = SlText.Caption,
            color = SlColor.TextTertiary,
        )
    }
}

/** 길로 보여 준 조건 말고 켜 둔 나머지 — 칩과 규칙 한 줄. */
@Composable
private fun OtherConditions(hero: UnlockCondition, stat: DailyStat, settings: LockSettings) {
    val others = enabledConditions(settings).filter { it != hero }
    if (others.isEmpty()) return
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        others.forEach { condition ->
            val achieved = condition.isAchieved(stat, settings)
            StatusChip(
                icon = when {
                    achieved -> SlIcons.CheckThin
                    condition == UnlockCondition.Sleep -> SlIcons.Moon
                    condition == UnlockCondition.Pomodoro -> SlIcons.TimerCompact
                    else -> SlIcons.Steps
                },
                text = when (condition) {
                    UnlockCondition.Sleep -> if (achieved) {
                        stringResource(R.string.lock_chip_sleep_done)
                    } else {
                        stringResource(R.string.lock_chip_sleep_goal, sleepGoalLabel(settings.sleepGoalHours))
                    }
                    UnlockCondition.Pomodoro -> stringResource(
                        R.string.lock_chip_sessions,
                        stat.pomodoroSessions,
                        settings.pomodoroGoal,
                    )
                    UnlockCondition.Steps -> stringResource(
                        R.string.unit_steps,
                        stat.steps.formatThousands(),
                    )
                },
                state = if (achieved) ChipState.Achieved else ChipState.Idle,
            )
        }
        Text(
            text = stringResource(
                if (settings.requireAllConditions) R.string.lock_rule_all else R.string.lock_rule_any,
            ),
            style = SlText.LabelSm,
            color = SlColor.TextTertiary,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
private fun FootNote(text: String) {
    Text(
        text = text,
        style = SlText.LinkSm,
        color = SlColor.TextTertiary,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
    )
}

/** 걸음 목표를 4등분한 이정표 — 8,000보면 2k · 4k · 6k. */
private fun lockTicks(goal: Int): List<String> = (1..3).map { i ->
    val value = goal * i / 4f / 1000f
    if (value % 1f == 0f) "${value.toInt()}k" else "%.1fk".format(java.util.Locale.US, value)
}

/** [endsAt] 까지 남은 시간(ms). 1초마다 다시 셉니다. 없으면 0. */
@Composable
private fun rememberRemaining(endsAt: Long?): Long {
    val remaining by produceState(
        initialValue = endsAt?.let { (it - System.currentTimeMillis()).coerceAtLeast(0L) } ?: 0L,
        endsAt,
    ) {
        if (endsAt == null) {
            value = 0L
            return@produceState
        }
        while (true) {
            value = (endsAt - System.currentTimeMillis()).coerceAtLeast(0L)
            if (value == 0L) break
            delay(1_000)
        }
    }
    return remaining
}

@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun LockOverlayScreenPreview() {
    LockOverlayScreen(
        appName = "YouTube",
        stat = SampleData.today,
        settings = SampleData.settings,
        temporaryAllowRemaining = TemporaryAllow.DAILY_LIMIT,
        onDismiss = {},
        onTemporaryAllow = {},
        usedTodayMinutes = 32,
    )
}

/** 집중 중이라 잠긴 상태 — 길 대신 남은 시간과 25분 칸. */
@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun LockOverlayScreenFocusPreview() {
    LockOverlayScreen(
        appName = "YouTube",
        stat = SampleData.today,
        settings = SampleData.settings,
        temporaryAllowRemaining = TemporaryAllow.DAILY_LIMIT,
        onDismiss = {},
        onTemporaryAllow = {},
        focusEndsAt = System.currentTimeMillis() + 18 * 60_000L,
        onOpenFocus = {},
    )
}

/** 기본 한도를 다 써서 광고 줄로 바뀐 상태. */
@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun LockOverlayScreenAdBonusPreview() {
    LockOverlayScreen(
        appName = "YouTube",
        stat = SampleData.today,
        settings = SampleData.settings,
        temporaryAllowRemaining = 0,
        onDismiss = {},
        onTemporaryAllow = {},
        adBonusRemaining = TemporaryAllow.AD_BONUS_LIMIT,
        onWatchAdForBonus = {},
    )
}
