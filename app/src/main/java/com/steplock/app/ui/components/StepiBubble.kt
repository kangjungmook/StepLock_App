package com.steplock.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.steplock.app.ui.theme.SlColor
import com.steplock.app.ui.theme.SlDimen
import com.steplock.app.ui.theme.SlText
import com.steplock.app.ui.theme.StepLockTheme

/** 말풍선 꼬리가 스텝이를 가리키는 방향. */
enum class BubbleTail {
    /** 스텝이가 말풍선 아래에 있을 때. */
    Down,

    /** 스텝이가 말풍선 왼쪽(시작 쪽)에 있을 때. */
    Start,
}

/**
 * 스텝이가 하는 말. 꼬리가 캐릭터를 가리켜서, 따로 "스텝이:" 라고 붙이지 않아도
 * 누구의 말인지 읽힙니다.
 *
 * 캐릭터가 **바로 옆에 있는 자리에서만** 씁니다. 캐릭터 없이 말풍선만 있으면
 * 그냥 테두리 있는 상자가 하나 더 생길 뿐입니다.
 */
@Composable
fun StepiBubble(
    modifier: Modifier = Modifier,
    tail: BubbleTail = BubbleTail.Down,
    containerColor: Color = SlColor.Surface,
    borderColor: Color = SlColor.Border,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(SlDimen.RadiusCard)
    val body: @Composable (Modifier) -> Unit = { bodyModifier ->
        Column(
            modifier = bodyModifier
                .clip(shape)
                .background(containerColor)
                .border(1.dp, borderColor, shape)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) { content() }
    }

    when (tail) {
        BubbleTail.Down -> Box(modifier = modifier, contentAlignment = Alignment.BottomCenter) {
            body(Modifier)
            BubbleTailShape(
                containerColor = containerColor,
                borderColor = borderColor,
                pointsLeft = false,
                // 테두리를 1dp 덮어 말풍선과 한 덩어리로 보이게 합니다.
                modifier = Modifier
                    .size(width = 20.dp, height = 11.dp)
                    .offset(y = 10.dp),
            )
        }

        BubbleTail.Start -> Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
            BubbleTailShape(
                containerColor = containerColor,
                borderColor = borderColor,
                pointsLeft = true,
                modifier = Modifier
                    .size(width = 11.dp, height = 20.dp)
                    .offset(x = 1.dp),
            )
            body(Modifier)
        }
    }
}

/** 한 줄짜리 말. 대부분의 자리는 이것만으로 충분합니다. */
@Composable
fun StepiSays(
    text: String,
    modifier: Modifier = Modifier,
    tail: BubbleTail = BubbleTail.Down,
    style: TextStyle = SlText.BodySm,
    textColor: Color = SlColor.TextPrimary,
    containerColor: Color = SlColor.Surface,
    borderColor: Color = SlColor.Border,
    textAlign: TextAlign = TextAlign.Center,
) {
    StepiBubble(
        modifier = modifier,
        tail = tail,
        containerColor = containerColor,
        borderColor = borderColor,
    ) {
        Text(text = text, style = style, color = textColor, textAlign = textAlign)
    }
}

@Composable
private fun BubbleTailShape(
    containerColor: Color,
    borderColor: Color,
    pointsLeft: Boolean,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // 꼬리의 두 변(바깥쪽)만 테두리를 긋습니다. 붙는 변은 말풍선과 이어집니다.
        val (a, tip, b) = if (pointsLeft) {
            Triple(Offset(w, 0f), Offset(0f, h / 2f), Offset(w, h))
        } else {
            Triple(Offset(0f, 0f), Offset(w / 2f, h), Offset(w, 0f))
        }
        val path = Path().apply {
            moveTo(a.x, a.y)
            lineTo(tip.x, tip.y)
            lineTo(b.x, b.y)
            close()
        }
        drawPath(path, containerColor)
        val stroke = 1.dp.toPx()
        drawLine(borderColor, a, tip, stroke)
        drawLine(borderColor, b, tip, stroke)
    }
}

@Preview
@Composable
private fun StepiBubblePreview() {
    StepLockTheme {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
            StepLockMascot(modifier = Modifier.size(width = 56.dp, height = 69.dp))
            StepiSays(text = "2,760보만 더 걸으면 열어 드릴게요", tail = BubbleTail.Start)
        }
    }
}
