package com.steplock.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.steplock.app.data.Pomodoro
import com.steplock.app.ui.theme.SlColor
import com.steplock.app.ui.theme.StepLockTheme

/**
 * 집중 세션 25분을 **25칸**으로 — 한 칸이 1분입니다.
 *
 * 줄어드는 링은 "얼마나 남았나"를 각도로 어림하게 하지만, 칸은 세어집니다. 지난 분은
 * 주황으로 칠해지고 지금 분은 지난 만큼만 차며, 남은 분은 흐린 회색입니다. 홈의
 * 등산로 표식 칸과 같은 모양이라, 걸음과 집중이 같은 "칸을 채우는 일"로 읽힙니다.
 *
 * 5칸 × 5줄. 줄마다 5분이라 "세 줄 남았다 = 15분"이 눈으로 나눠집니다.
 */
@Composable
fun MinuteGrid(
    remainingMs: Long,
    description: String,
    modifier: Modifier = Modifier,
    filledColor: Color = SlColor.Brand,
    emptyColor: Color = SlColor.TrackOff,
) {
    val total = Pomodoro.SESSION_MINUTES
    val elapsedMinutes = ((Pomodoro.SESSION_MS - remainingMs).coerceIn(0L, Pomodoro.SESSION_MS)) / 60_000f
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(GRID_HEIGHT)
            .semantics { contentDescription = description },
    ) {
        val columns = 5
        val rows = (total + columns - 1) / columns
        val gap = 6.dp.toPx()
        val cellW = (size.width - gap * (columns - 1)) / columns
        val cellH = (size.height - gap * (rows - 1)) / rows
        val radius = CornerRadius(4.dp.toPx())
        for (i in 0 until total) {
            val left = (i % columns) * (cellW + gap)
            val top = (i / columns) * (cellH + gap)
            drawRoundRect(emptyColor, Offset(left, top), Size(cellW, cellH), radius)
            val part = (elapsedMinutes - i).coerceIn(0f, 1f)
            if (part > 0f) {
                drawRoundRect(filledColor, Offset(left, top), Size(cellW * part, cellH), radius)
            }
        }
    }
}

private val GRID_HEIGHT = 28.dp * 5 + 6.dp * 4

@Preview(widthDp = 372)
@Composable
private fun MinuteGridPreview() {
    StepLockTheme {
        MinuteGrid(remainingMs = 14 * 60_000L + 13_000L, description = "")
    }
}
