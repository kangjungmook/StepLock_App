package com.steplock.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.steplock.app.ui.theme.SlColor
import com.steplock.app.ui.theme.StepLockTheme
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * 캐릭터의 상태.
 *
 * - [Walking] 조건 미달 — 목표까지 걸어갑니다
 * - [Resting] 달성 — 서서 눈을 감고 쉽니다
 * - [Focusing] 집중 세션 진행 중 — 앉아서 숨 쉬는 정도만 움직입니다
 *
 * [Resting] 은 어떤 애니메이션 값도 읽지 않아서 완전히 정지합니다.
 * 멈춘 상태에 미세한 움직임이 남아 있으면 그게 더 눈에 걸립니다.
 */
enum class MascotMood { Walking, Resting, Focusing }

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
    val focusing = mood == MascotMood.Focusing
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
    // 숨결. 걷기(900ms)의 세 배 가까이 느리고 진폭도 절반이라, 25분을 켜 둬도
    // 시선을 끌지 않습니다. Reverse 로 왕복해야 들이쉬고 내쉬는 것처럼 보입니다.
    val breath by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "mascotBreath",
    )

    Canvas(modifier = modifier) {
        val unit = size.width / DESIGN_WIDTH
        fun x(value: Float) = value * unit
        fun y(value: Float) = value * unit

        val cycle = if (walking) phase * 2f * Math.PI.toFloat() else 0f
        val swing = if (walking) sin(cycle) * 20f else 0f
        // Resting 은 두 애니메이션 값을 **읽지 않습니다** — 읽으면 값이 바뀔 때마다
        // 다시 그려져서, 화면에는 멈춰 있는데 매 프레임 일을 하게 됩니다.
        val bob = when {
            walking -> -abs(sin(cycle)) * 3f
            focusing -> -breath * 1.6f
            else -> 0f
        }
        // 앉으면 몸이 바닥 쪽으로 내려옵니다.
        val drop = if (focusing) 8f else 0f
        val lift = bob + drop

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
                start = Offset(x(hipX), y(94f + lift)),
                end = Offset(
                    x(hipX + 15f * sin(radians)),
                    y(94f + lift + 15f * cos(radians)),
                ),
                strokeWidth = x(11f),
                cap = StrokeCap.Round,
            )
        }

        if (focusing) {
            // 앉아서 앞으로 내놓은 두 발.
            //
            // 하나의 넓은 바로 그리면 받침대 위에 서 있는 것처럼 보이고, 다리를
            // X 로 포개면 엉킨 덩어리가 됩니다(둘 다 그려서 확인했습니다).
            // **가운데를 비운 두 덩이**여야 발로 읽힙니다.
            listOf(30f to 41f, 59f to 70f).forEach { (startX, endX) ->
                drawLine(
                    color = bodyColor,
                    start = Offset(x(startX), y(113f)),
                    end = Offset(x(endX), y(113f)),
                    strokeWidth = x(11f),
                    cap = StrokeCap.Round,
                )
            }
        } else {
            // 두 다리가 서로 반대 방향으로 벌어져야 걷는 것처럼 보입니다.
            // 같은 쪽으로 돌면 다리가 겹칩니다.
            leg(hipX = 40f, angleDegrees = -swing)
            leg(hipX = 60f, angleDegrees = swing)
        }

        // 뒤쪽으로 살짝 밀려난 몸통이 입체감을 만듭니다.
        drawRoundRect(
            color = shadeColor,
            topLeft = Offset(x(8f), y(46f + lift)),
            size = Size(x(72f), y(54f)),
            cornerRadius = CornerRadius(x(19f)),
        )
        shackle(centerX = 50f, bob = lift, color = shadeColor, unit = unit)
        shackle(centerX = 54f, bob = lift, color = bodyColor, unit = unit)
        drawRoundRect(
            color = bodyColor,
            topLeft = Offset(x(16f), y(44f + lift)),
            size = Size(x(72f), y(54f)),
            cornerRadius = CornerRadius(x(19f)),
        )

        if (walking) {
            drawCircle(eyeColor, radius = x(4.6f), center = Offset(x(44f), y(70f + lift)))
            drawCircle(eyeColor, radius = x(4.6f), center = Offset(x(66f), y(68f + lift)))
        } else {
            // 쉬는 표정 — 감은 눈
            listOf(44f to 70f, 66f to 68f).forEach { (eyeX, eyeY) ->
                drawLine(
                    color = eyeColor,
                    start = Offset(x(eyeX - 4.6f), y(eyeY + lift)),
                    end = Offset(x(eyeX + 4.6f), y(eyeY + lift)),
                    strokeWidth = x(2.4f),
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

private fun DrawScope.shackle(
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

@Preview
@Composable
private fun StepLockMascotPreview() {
    StepLockTheme {
        StepLockMascot(modifier = Modifier.size(width = 120.dp, height = 149.dp))
    }
}

@Preview
@Composable
private fun StepLockMascotFocusingPreview() {
    StepLockTheme {
        StepLockMascot(
            modifier = Modifier.size(width = 120.dp, height = 149.dp),
            mood = MascotMood.Focusing,
        )
    }
}
