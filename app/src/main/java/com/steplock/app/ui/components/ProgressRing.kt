package com.steplock.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import com.steplock.app.ui.theme.SlColor

/**
 * 홈 44dp · 잠금 200dp에서 같이 쓰는 게이지.
 * 시안의 SVG와 맞추기 위해 링 반지름을 박스 크기와 따로 받습니다.
 */
@Composable
fun ProgressRing(
    progress: Float,
    size: Dp,
    radius: Dp,
    strokeWidth: Dp,
    modifier: Modifier = Modifier,
    trackColor: Color = SlColor.SurfaceAlt,
    progressColor: Color = SlColor.Brand,
    content: @Composable BoxScope.() -> Unit = {},
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val ringRadius = radius.toPx()
            val stroke = strokeWidth.toPx()
            val topLeft = Offset(center.x - ringRadius, center.y - ringRadius)
            val arcSize = Size(ringRadius * 2, ringRadius * 2)

            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke),
            )
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        content()
    }
}
