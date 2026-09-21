package com.steplock.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.steplock.app.ui.theme.SlColor
import com.steplock.app.ui.theme.SlText
import com.steplock.app.ui.theme.StepLockTheme

/**
 * 오늘 목표까지 얼마나 왔는지 — 캐릭터의 **위치**로 보여 줍니다.
 *
 * 진행률을 링이나 막대로 그리면 어느 앱에서나 본 모양이 됩니다. 이 앱의 캐릭터는
 * 다리가 달린 자물쇠이고 이름도 "걸어서 잠금을 푼다"는 뜻이라, 걸어온 거리를
 * 그대로 위치로 쓰는 편이 이 앱만의 형태가 됩니다.
 *
 * 지나온 길은 이어진 선, 남은 길은 점선, 끝에는 결승선을 둡니다.
 * 담을 상자가 없고 가로로 길어서, 세로로 쌓인 카드들 사이에서 리듬이 바뀝니다.
 */
@Composable
fun StepTrack(
    progress: Float,
    startLabel: String,
    goalLabel: String,
    modifier: Modifier = Modifier,
    mood: MascotMood = MascotMood.Walking,
) {
    val clamped = progress.coerceIn(0f, 1f)
    // 걸음이 올라갈 때 캐릭터가 순간이동하지 않고 걸어서 이동합니다.
    val animated by animateFloatAsState(
        targetValue = clamped,
        animationSpec = tween(durationMillis = 600),
        label = "trackProgress",
    )

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val trackWidth = maxWidth
        val walkWidth = trackWidth - MASCOT_WIDTH

        Column {
            // 캐릭터 그림 아래쪽에 발자국용 여백이 있어서, 그 만큼 낮은 칸에 두어
            // 발이 트랙 선에 닿게 합니다(칸을 넘겨 그려도 잘리지 않습니다).
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MASCOT_HEIGHT - 6.dp),
            ) {
                StepLockMascot(
                    modifier = Modifier
                        .offset(x = walkWidth * animated)
                        .size(width = MASCOT_WIDTH, height = MASCOT_HEIGHT),
                    mood = mood,
                )
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TRACK_HEIGHT),
            ) {
                val stroke = 6.dp.toPx()
                val centerY = size.height / 2f
                // 결승선이 잘리지 않도록 선 자체는 그 앞에서 끝냅니다.
                val finishX = size.width - 2.dp.toPx()
                val walked = finishX * animated

                if (animated < 1f) {
                    drawLine(
                        color = SlColor.BorderStrong,
                        start = Offset(walked + stroke, centerY),
                        end = Offset(finishX, centerY),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(2.dp.toPx(), 6.dp.toPx()),
                            0f,
                        ),
                    )
                }
                if (animated > 0f) {
                    drawLine(
                        color = SlColor.Brand,
                        start = Offset(0f, centerY),
                        end = Offset(walked, centerY),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round,
                    )
                }
                // 결승선 — 목표에 닿으면 브랜드 색으로 바뀝니다.
                drawLine(
                    color = if (animated >= 1f) SlColor.Brand else SlColor.BorderStrong,
                    start = Offset(finishX, 0f),
                    end = Offset(finishX, size.height),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }

            Spacer(Modifier.height(10.dp))
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
private val TRACK_HEIGHT = 16.dp

@Preview(widthDp = 372)
@Composable
private fun StepTrackPreview() {
    StepLockTheme {
        StepTrack(progress = 0.66f, startLabel = "5,240보", goalLabel = "목표 8,000보")
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
