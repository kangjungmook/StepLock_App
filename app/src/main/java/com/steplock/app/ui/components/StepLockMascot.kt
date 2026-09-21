package com.steplock.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CornerRadius
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.steplock.app.ui.theme.SlColor
import com.steplock.app.ui.theme.StepLockTheme
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** 걷거나(조건 미달) 가만히 서 있는(달성) 두 가지 상태만 씁니다. */
enum class MascotMood { Walking, Resting }

/**
 * 스텝락 캐릭터. 로고와 같은 자물쇠를 100×124 좌표계로 다시 그려
 * 다리 스윙과 상하 반동만 애니메이션합니다. 비트맵이 아니라 어떤 크기에서도 선명합니다.
 */
@Composable
fun StepLockMascot(
    modifier: Modifier = Modifier,
    mood: MascotMood = MascotMood.Walking,
    bodyColor: Color = SlColor.Brand,
    shadeColor: Color = SlColor.BrandInk,
    eyeColor: Color = SlColor.BrandDeep,
    footprintColor: Color = SlColor.Border,
) {
    val walking = mood == MascotMood.Walking
    val transition = rememberInfiniteTransition(label = "mascot")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "mascotPhase",
    )

    Canvas(modifier = modifier) {
        val unit = size.width / DESIGN_WIDTH
        fun x(value: Float) = value * unit
        fun y(value: Float) = value * unit

        val cycle = if (walking) phase * 2f * Math.PI.toFloat() else 0f
        val swing = if (walking) sin(cycle) * 20f else 0f
        val bob = if (walking) -abs(sin(cycle)) * 3f else 0f

        if (walking) {
            drawOval(
                color = footprintColor,
                topLeft = Offset(x(27f), y(114.4f)),
                size = Size(x(14f), y(5.2f)),
                alpha = 0.9f,
            )
            drawOval(
                color = footprintColor,
                topLeft = Offset(x(14f), y(116.6f)),
                size = Size(x(12f), y(4.8f)),
                alpha = 0.5f,
            )
        }

        fun leg(hipX: Float, angleDegrees: Float) {
            val radians = Math.toRadians(angleDegrees.toDouble()).toFloat()
            drawLine(
                color = bodyColor,
                start = Offset(x(hipX), y(94f + bob)),
                end = Offset(
                    x(hipX + 15f * sin(radians)),
                    y(94f + bob + 15f * cos(radians)),
                ),
                strokeWidth = x(11f),
                cap = StrokeCap.Round,
            )
        }
        // 두 다리가 서로 반대 방향으로 벌어져야 걷는 것처럼 보입니다. 같은 쪽으로 돌면 다리가 겹칩니다.
        leg(hipX = 40f, angleDegrees = -swing)
        leg(hipX = 60f, angleDegrees = swing)

        // 뒤쪽으로 살짝 밀려난 몸통이 입체감을 만듭니다.
        drawRoundRect(
            color = shadeColor,
            topLeft = Offset(x(8f), y(46f + bob)),
            size = Size(x(72f), y(54f)),
            cornerRadius = CornerRadius(x(19f)),
        )
        shackle(centerX = 50f, bob = bob, color = shadeColor, unit = unit)
        shackle(centerX = 54f, bob = bob, color = bodyColor, unit = unit)
        drawRoundRect(
            color = bodyColor,
            topLeft = Offset(x(16f), y(44f + bob)),
            size = Size(x(72f), y(54f)),
            cornerRadius = CornerRadius(x(19f)),
        )

        if (walking) {
            drawCircle(eyeColor, radius = x(4.6f), center = Offset(x(44f), y(70f + bob)))
            drawCircle(eyeColor, radius = x(4.6f), center = Offset(x(66f), y(68f + bob)))
        } else {
            // 쉬는 표정 — 감은 눈
            listOf(44f to 70f, 66f to 68f).forEach { (eyeX, eyeY) ->
                drawLine(
                    color = eyeColor,
                    start = Offset(x(eyeX - 4.6f), y(eyeY + bob)),
                    end = Offset(x(eyeX + 4.6f), y(eyeY + bob)),
                    strokeWidth = x(2.4f),
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.shackle(
    centerX: Float,
    bob: Float,
    color: Color,
    unit: Float,
) {
    val radius = 20f
    val stroke = Stroke(width = 11f * unit, cap = StrokeCap.Round)
    val arcRect = Rect(
        left = (centerX - radius) * unit,
        top = (12f + bob) * unit,
        right = (centerX + radius) * unit,
        bottom = (32f + radius + bob) * unit,
    )
    drawArc(
        color = color,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(arcRect.left, arcRect.top),
        size = Size(arcRect.width, arcRect.height),
        style = stroke,
    )
    listOf(centerX - radius, centerX + radius).forEach { legX ->
        drawLine(
            color = color,
            start = Offset(legX * unit, (32f + bob) * unit),
            end = Offset(legX * unit, (48f + bob) * unit),
            strokeWidth = 11f * unit,
            cap = StrokeCap.Round,
        )
    }
}

private const val DESIGN_WIDTH = 100f

/** 캐릭터 전체가 담기려면 가로:세로가 100:124여야 합니다. */
const val MASCOT_ASPECT_RATIO = 100f / 124f

@Preview
@Composable
private fun StepLockMascotPreview() {
    StepLockTheme {
        StepLockMascot(modifier = Modifier.size(width = 120.dp, height = 149.dp))
    }
}
