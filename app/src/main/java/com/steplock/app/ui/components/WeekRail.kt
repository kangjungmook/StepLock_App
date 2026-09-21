package com.steplock.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.steplock.app.R
import com.steplock.app.ui.theme.SlColor
import com.steplock.app.ui.theme.SlText
import com.steplock.app.ui.theme.StepLockTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** 하루치 표시에 필요한 것 — 날짜와 그날 잠금이 풀렸는지. */
data class WeekDay(val date: LocalDate, val unlocked: Boolean)

/**
 * 최근 7일을 한 줄로. 연속 달성을 숫자로만 말하면 실감이 없어서, 어느 날을
 * 채웠고 어느 날을 놓쳤는지 눈으로 보이게 합니다.
 *
 * 어두운 헤더 안에 들어가므로 색을 인자로 받습니다.
 */
@Composable
fun WeekRail(
    days: List<WeekDay>,
    modifier: Modifier = Modifier,
    labelColor: Color = SlColor.Dark.TextMuted,
    dateColor: Color = SlColor.Dark.TextPrimary,
    todayContainer: Color = SlColor.Brand,
    todayContent: Color = SlColor.OnBrand,
    markColor: Color = SlColor.Dark.GreenIcon,
    emptyMarkColor: Color = SlColor.Dark.Border,
) {
    val today = LocalDate.now()
    val weekdayFormatter = DateTimeFormatter.ofPattern(
        stringResource(R.string.stats_weekday_pattern),
        Locale.KOREAN,
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        days.forEach { day ->
            val isToday = day.date == today
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = day.date.format(weekdayFormatter),
                    style = SlText.LabelSm,
                    color = labelColor,
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (isToday) todayContainer else Color.Transparent),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = day.date.dayOfMonth.toString(),
                        style = SlText.RowTitle,
                        color = if (isToday) todayContent else dateColor,
                    )
                }
                Spacer(Modifier.height(6.dp))
                // 채운 날만 점이 찍힙니다. 오늘은 아직 진행 중일 수 있습니다.
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(if (day.unlocked) markColor else emptyMarkColor),
                )
            }
        }
    }
}

@Preview(widthDp = 372)
@Composable
private fun WeekRailPreview() {
    StepLockTheme {
        Box(modifier = Modifier.background(SlColor.Dark.Background)) {
            WeekRail(
                days = (6 downTo 0).map { offset ->
                    WeekDay(
                        date = LocalDate.now().minusDays(offset.toLong()),
                        unlocked = offset in 1..5,
                    )
                },
            )
        }
    }
}
