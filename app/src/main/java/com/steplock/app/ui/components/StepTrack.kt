package com.steplock.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.steplock.app.ui.theme.SlDimen
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.steplock.app.ui.theme.SlColor
import com.steplock.app.ui.theme.SlText
import com.steplock.app.ui.theme.StepLockTheme

/**
 * 오늘 목표까지 얼마나 왔는지 — 캐릭터의 **위치**로 보여 줍니다.
 *
 * 진행률을 링으로 그리면 어느 앱에서나 본 모양이 됩니다. 이 앱의 캐릭터는
 * 다리가 달린 자물쇠이고 이름도 "걸어서 잠금을 푼다"는 뜻이라, 걸어온 거리를
 * 그대로 위치로 쓰는 편이 이 앱만의 형태가 됩니다.
 *
 * 굵은 길(8dp) 위를 스텝이가 걷고, 길 끝에는 자물쇠 표시가 있습니다. [ticks] 를
 * 주면 길을 4등분한 자리에 이정표(2k · 4k · 6k)를 세웁니다 — "얼마나 남았나"를
 * 숫자를 읽지 않고도 가늠할 수 있게 합니다.
 */
@Composable
fun StepTrack(
    progress: Float,
    startLabel: String,
    goalLabel: String,
    modifier: Modifier = Modifier,
    mood: MascotMood = MascotMood.Walking,
    /** 1/4 · 2/4 · 3/4 자리의 이정표 글자. 비우면 이정표를 그리지 않습니다. */
    ticks: List<String> = emptyList(),
) {
    val clamped = progress.coerceIn(0f, 1f)
    // 걸음이 올라갈 때 캐릭터가 순간이동하지 않고 걸어서 이동합니다.
    val animated by animateFloatAsState(
        targetValue = clamped,
        animationSpec = tween(durationMillis = 600),
        label = "trackProgress",
    )

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // 길은 끝의 자물쇠 기둥 앞에서 끝납니다.
        val roadWidth = maxWidth - GOAL_MARK - 6.dp
        val walkWidth = roadWidth - MASCOT_WIDTH / 2

        Column {
            // 캐릭터 그림 아래쪽에 발자국용 여백이 있어서, 그 만큼 낮은 칸에 두어
            // 발이 길에 닿게 합니다(칸을 넘겨 그려도 잘리지 않습니다).
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MASCOT_HEIGHT - 8.dp),
            ) {
                StepLockMascot(
                    modifier = Modifier
                        .offset(x = (walkWidth * animated - MASCOT_WIDTH / 2).coerceAtLeast(0.dp))
                        .size(width = MASCOT_WIDTH, height = MASCOT_HEIGHT),
                    mood = mood,
                )
            }

            // 그리기 블록은 컴포저블이 아니라 테마 색을 그 안에서 읽을 수 없습니다.
            val markOn = SlColor.Brand
            val markOff = SlColor.TrackOff
            val tickColor = SlColor.TextTertiary
            val done = animated >= 1f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(GOAL_MARK),
            ) {
                // 길은 **칠해 둔 표식의 줄**입니다. 산길 나무에 주황 페인트로 그어 둔
                // 표식처럼, 걸은 만큼의 칸이 주황으로 칠해지고 남은 칸은 흐린 회색입니다.
                // 칸 사이 틈 덕에 "몇 칸 남았나"가 숫자를 읽지 않아도 세어집니다.
                Canvas(modifier = Modifier.matchParentSize()) {
                    val end = size.width - GOAL_MARK.toPx() - 6.dp.toPx()
                    val gap = 3.dp.toPx()
                    val markWidth = (end - gap * (MARKS - 1)) / MARKS
                    val markHeight = 10.dp.toPx()
                    val top = (size.height - markHeight) / 2f
                    val filled = animated * MARKS
                    for (i in 0 until MARKS) {
                        val left = i * (markWidth + gap)
                        drawRoundRect(
                            color = markOff,
                            topLeft = Offset(left, top),
                            size = Size(markWidth, markHeight),
                            cornerRadius = CornerRadius(2.dp.toPx()),
                        )
                        // 지금 걷는 칸은 걸은 만큼만 칠합니다 — 칸 단위로 뚝뚝 끊기지 않게.
                        val part = (filled - i).coerceIn(0f, 1f)
                        if (part > 0f) {
                            drawRoundRect(
                                color = markOn,
                                topLeft = Offset(left, top),
                                size = Size(markWidth * part, markHeight),
                                cornerRadius = CornerRadius(2.dp.toPx()),
                            )
                        }
                    }
                    if (ticks.isNotEmpty()) {
                        for (q in 1..3) {
                            val x = end * q / 4f
                            drawLine(
                                color = tickColor,
                                start = Offset(x, top - 6.dp.toPx()),
                                end = Offset(x, top - 2.dp.toPx()),
                                strokeWidth = 1.5.dp.toPx(),
                            )
                        }
                    }
                }
                // 길 끝의 자물쇠 기둥 — 목표에 닿으면 주황으로 칠해집니다.
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(GOAL_MARK)
                        .clip(RoundedCornerShape(SlDimen.RadiusSmall))
                        .background(if (done) SlColor.Brand else SlColor.TextPrimary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = SlIcons.PasswordLock,
                        contentDescription = null,
                        tint = if (done) SlColor.OnBrand else SlColor.Background,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }

            if (ticks.size == 3) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp),
                ) {
                    ticks.forEachIndexed { index, label ->
                        Text(
                            text = label,
                            style = SlText.LabelSm,
                            color = SlColor.TextTertiary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .width(TICK_LABEL_WIDTH)
                                .offset(x = roadWidth * (index + 1) / 4f - TICK_LABEL_WIDTH / 2),
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(text = startLabel, style = SlText.Remaining, color = SlColor.TextPrimary)
                Spacer(Modifier.weight(1f))
                Text(text = goalLabel, style = SlText.LabelSm, color = SlColor.TextSecondary)
            }
        }
    }
}

private val MASCOT_WIDTH = 44.dp
private val MASCOT_HEIGHT = 55.dp
private val GOAL_MARK = 28.dp
private val TICK_LABEL_WIDTH = 40.dp

/** 표식 칸 수. 24칸이면 8,000보 목표에서 한 칸이 약 330보 — 한 블록쯤 걸으면 한 칸이 찹니다. */
private const val MARKS = 24

@Preview(widthDp = 372)
@Composable
private fun StepTrackPreview() {
    StepLockTheme {
        StepTrack(
            progress = 0.66f,
            startLabel = "5,240보",
            goalLabel = "목표 8,000보",
            ticks = listOf("2k", "4k", "6k"),
        )
    }
}

@Preview(widthDp = 372)
@Composable
private fun StepTrackDonePreview() {
    StepLockTheme {
        StepTrack(
            progress = 1f,
            startLabel = "8,240보",
            goalLabel = "목표 8,000보",
            mood = MascotMood.Resting,
        )
    }
}
