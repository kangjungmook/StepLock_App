package com.steplock.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.steplock.app.ui.theme.SlColor
import com.steplock.app.ui.theme.SlDimen
import com.steplock.app.ui.theme.SlText
import com.steplock.app.ui.theme.StepLockTheme
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * 홈의 "등산로" — 화면 아래에서 위로 굽이쳐 올라가는 길과, 그 위에 선 스텝이.
 *
 * 길은 산길 나무에 칠한 **표식 칸**으로 그립니다. 걸은 만큼의 칸이 주황으로 칠해지고,
 * 남은 칸은 흐린 회색, 맨 위(정상)에는 흑연색 자물쇠 기둥이 있습니다. 진행 막대를
 * 옆으로 눕히는 대신 **위로 오르는 길**로 세워서, 목표에 다가가는 것이 "올라간다"로
 * 읽히게 했습니다 — 이 앱만의 첫 화면입니다.
 *
 * 길의 모양은 두 구간의 3차 베지에 곡선이고, 칸은 곡선의 **길이 기준으로 고르게** 놓습니다
 * (매개변수 t 로 나누면 굽은 곳에서 칸이 몰립니다).
 *
 * @param progress 0~1. 걸은 만큼.
 * @param ticks 1/4 · 2/4 · 3/4 자리의 이정표 글자(예: 2k · 4k · 6k). 비우면 생략.
 */
@Composable
fun TrailMap(
    progress: Float,
    modifier: Modifier = Modifier,
    ticks: List<String> = emptyList(),
    mood: MascotMood = MascotMood.Walking,
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 700),
        label = "trailProgress",
    )
    val textMeasurer = rememberTextMeasurer()
    val tickStyle = SlText.TrailTick.copy(color = SlColor.TextTertiary)
    val markOn = SlColor.Brand
    val markOff = SlColor.TrackOff

    BoxWithConstraints(modifier = modifier) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val trail = remember(widthPx, heightPx) { Trail(widthPx, heightPx) }
        val density = androidx.compose.ui.platform.LocalDensity.current

        Canvas(modifier = Modifier.fillMaxSize()) {
            val markW = 14.dp.toPx()
            val markH = 8.dp.toPx()
            for (i in 0 until MARKS) {
                val f = (i + 0.5f) / MARKS
                val p = trail.at(f * ROAD_END)
                val on = f <= animated
                rotate(p.angle, pivot = Offset(p.x, p.y)) {
                    drawRoundRect(
                        color = if (on) markOn else markOff,
                        topLeft = Offset(p.x - markW / 2, p.y - markH / 2),
                        size = Size(markW, markH),
                        cornerRadius = CornerRadius(2.dp.toPx()),
                    )
                }
            }
            if (ticks.size == 3) {
                ticks.forEachIndexed { index, label ->
                    val p = trail.at((index + 1) / 4f * ROAD_END)
                    val layout = textMeasurer.measure(label, tickStyle)
                    drawText(
                        textLayoutResult = layout,
                        topLeft = Offset(
                            p.x - 18.dp.toPx() - layout.size.width,
                            p.y - layout.size.height / 2f,
                        ),
                    )
                }
            }
        }

        // 정상의 자물쇠 기둥 — 목표에 닿으면 주황으로 칠해집니다.
        val done = animated >= 1f
        val top = trail.at(1f)
        Box(
            modifier = Modifier
                .offset { IntOffset((top.x - LOCK_HALF.toPx(density)).roundToInt(), (top.y - LOCK_HALF.toPx(density) * 2).roundToInt()) }
                .size(LOCK_HALF * 2)
                .clip(RoundedCornerShape(SlDimen.RadiusSmall))
                .background(if (done) SlColor.Brand else SlColor.TextPrimary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = SlIcons.PasswordLock,
                contentDescription = null,
                tint = if (done) SlColor.OnBrand else SlColor.Background,
                modifier = Modifier.size(16.dp),
            )
        }

        // 스텝이 — 걸은 만큼의 자리에 섭니다. 발이 길에 닿도록 그림 높이만큼 올립니다.
        val here = trail.at(animated * ROAD_END)
        StepLockMascot(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (here.x - MASCOT_W.toPx(density) / 2).roundToInt(),
                        (here.y - MASCOT_H.toPx(density) + 6.dp.toPx(density)).roundToInt(),
                    )
                }
                .size(width = MASCOT_W, height = MASCOT_H),
            mood = mood,
        )
    }
}

private fun Dp.toPx(density: androidx.compose.ui.unit.Density) = with(density) { this@toPx.toPx() }

/** 길 위 한 점 — 위치와 그 자리의 방향(도). */
private data class TrailPoint(val x: Float, val y: Float, val angle: Float)

/**
 * 길의 모양. 기준 크기 372×460 에서 고른 점들을 실제 크기로 늘립니다.
 * 아래쪽(시작)은 오른쪽 아래, 위쪽(자물쇠)은 오른쪽 위 — 왼쪽에는 큰 숫자가 놓입니다.
 */
private class Trail(width: Float, height: Float) {
    private val points: List<Offset>
    private val lengths: FloatArray

    init {
        val sx = width / REF_W
        val sy = height / REF_H
        fun o(x: Float, y: Float) = Offset(x * sx, y * sy)
        val segments = listOf(
            listOf(o(280f, 440f), o(410f, 370f), o(150f, 330f), o(265f, 250f)),
            listOf(o(265f, 250f), o(380f, 170f), o(220f, 120f), o(332f, 52f)),
        )
        val pts = mutableListOf<Offset>()
        for (seg in segments) {
            for (i in 0 until SAMPLES) pts += cubic(seg[0], seg[1], seg[2], seg[3], i / SAMPLES.toFloat())
        }
        pts += segments.last()[3]
        points = pts
        lengths = FloatArray(pts.size)
        for (i in 1 until pts.size) {
            lengths[i] = lengths[i - 1] + hypot(pts[i].x - pts[i - 1].x, pts[i].y - pts[i - 1].y)
        }
    }

    /** 길 전체 길이의 [fraction] 지점. */
    fun at(fraction: Float): TrailPoint {
        val target = fraction.coerceIn(0f, 1f) * lengths.last()
        var i = 1
        while (i < lengths.size - 1 && lengths[i] < target) i++
        val a = points[i - 1]
        val b = points[i]
        val span = (lengths[i] - lengths[i - 1]).coerceAtLeast(1e-3f)
        val t = ((target - lengths[i - 1]) / span).coerceIn(0f, 1f)
        val angle = Math.toDegrees(atan2((b.y - a.y).toDouble(), (b.x - a.x).toDouble())).toFloat()
        return TrailPoint(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t, angle)
    }

    private fun cubic(p0: Offset, p1: Offset, p2: Offset, p3: Offset, t: Float): Offset {
        val u = 1 - t
        val a = u * u * u
        val b = 3 * u * u * t
        val c = 3 * u * t * t
        val d = t * t * t
        return Offset(
            a * p0.x + b * p1.x + c * p2.x + d * p3.x,
            a * p0.y + b * p1.y + c * p2.y + d * p3.y,
        )
    }

    private companion object {
        const val REF_W = 372f
        const val REF_H = 460f
        const val SAMPLES = 200
    }
}

/** 표식 칸 수. 8,000보 목표면 한 칸이 약 300보 — 한 블록 걸으면 한 칸이 찹니다. */
private const val MARKS = 26

/** 칸이 놓이는 마지막 지점. 그 위는 자물쇠 기둥 자리입니다. */
private const val ROAD_END = 0.94f

private val LOCK_HALF = 16.dp
private val MASCOT_W = 48.dp
private val MASCOT_H = 60.dp

@Preview(widthDp = 372, heightDp = 460)
@Composable
private fun TrailMapPreview() {
    StepLockTheme {
        TrailMap(
            progress = 0.66f,
            ticks = listOf("2k", "4k", "6k"),
            modifier = Modifier.fillMaxSize(),
        )
    }
}
