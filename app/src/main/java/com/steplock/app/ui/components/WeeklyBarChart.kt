package com.steplock.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.dp
import com.steplock.app.ui.theme.SlColor

/**
 * 일별 막대 하나에 값 하나. 영점에서 시작하고 축은 하나만 씁니다.
 * 목표는 점선으로만 표시하고, 값 라벨은 화면 쪽 캡션이 담당합니다.
 */
@Composable
fun WeeklyBarChart(
    values: List<Int>,
    goal: Int,
    modifier: Modifier = Modifier,
    barColor: Color = SlColor.Brand,
    emptyBarColor: Color = SlColor.Border,
    goalLineColor: Color = SlColor.BorderStrong,
    baselineColor: Color = SlColor.Border,
) {
    val scaleMax = maxOf(goal, values.maxOrNull() ?: 0).coerceAtLeast(1)

    Canvas(modifier = modifier.clipToBounds()) {
        if (values.isEmpty()) return@Canvas

        val slotWidth = size.width / values.size
        val barWidth = slotWidth * 0.46f
        val cornerPx = 4.dp.toPx()
        val stubPx = 2.dp.toPx()
        val plotHeight = size.height - stubPx

        val goalY = plotHeight * (1f - goal.toFloat() / scaleMax)
        drawLine(
            color = goalLineColor,
            start = Offset(0f, goalY),
            end = Offset(size.width, goalY),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx()), 0f),
        )

        values.forEachIndexed { index, value ->
            val barHeight = (plotHeight * (value.toFloat() / scaleMax)).coerceAtLeast(stubPx)
            val left = slotWidth * index + (slotWidth - barWidth) / 2f
            // 아래쪽 모서리는 경계 밖으로 넘겨 잘라내 기준선에 붙은 형태를 만듭니다.
            drawRoundRect(
                color = if (value == 0) emptyBarColor else barColor,
                topLeft = Offset(left, size.height - barHeight),
                size = Size(barWidth, barHeight + cornerPx),
                cornerRadius = CornerRadius(cornerPx, cornerPx),
            )
        }

        drawLine(
            color = baselineColor,
            start = Offset(0f, size.height),
            end = Offset(size.width, size.height),
            strokeWidth = 1.dp.toPx(),
        )
    }
}
