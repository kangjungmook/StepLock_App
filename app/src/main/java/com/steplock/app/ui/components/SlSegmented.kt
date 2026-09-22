package com.steplock.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.steplock.app.ui.theme.SlColor
import com.steplock.app.ui.theme.SlDimen
import com.steplock.app.ui.theme.SlText
import com.steplock.app.ui.theme.StepLockTheme

/**
 * 같은 내용을 다른 기준으로 볼 때 쓰는 전환 막대.
 *
 * 탭이 아닙니다 — 화면을 옮기지 않고 **보는 범위만** 바꿉니다. 그래서 하단 탭바와
 * 다른 모양(작은 알약)을 쓰고, 고른 칸만 면을 채워 어디에 있는지 분명히 합니다.
 *
 * 항목은 두세 개까지만 두세요. 네 개를 넘으면 글자가 좁아져 읽기 어려워집니다.
 */
@Composable
fun SlSegmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(SlDimen.RadiusField))
            .background(SlColor.SurfaceAlt)
            .padding(3.dp),
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(SlDimen.RadiusBadgeSmall))
                    .background(if (selected) SlColor.Surface else SlColor.SurfaceAlt)
                    .selectable(
                        selected = selected,
                        role = Role.Tab,
                        onClick = { onSelect(index) },
                    )
                    // 세로 10dp + 글자 높이로 터치 영역이 44dp 를 넘습니다.
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = SlText.CaptionMedium,
                    color = if (selected) SlColor.TextPrimary else SlColor.TextSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Preview(widthDp = 240)
@Composable
private fun SlSegmentedPreview() {
    StepLockTheme {
        SlSegmented(
            options = listOf("7일", "30일"),
            selectedIndex = 0,
            onSelect = {},
        )
    }
}
