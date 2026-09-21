package com.steplock.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.steplock.app.ui.theme.SlColor
import com.steplock.app.ui.theme.SlText

/** 잠금 화면의 조건별 미니 상태 — 달성 / 미달성. */
enum class ChipState { Achieved, Idle }

@Composable
fun StatusChip(
    icon: ImageVector,
    text: String,
    state: ChipState,
    modifier: Modifier = Modifier,
) {
    val container = if (state == ChipState.Achieved) SlColor.Dark.GreenTint else SlColor.Dark.Surface
    val border = if (state == ChipState.Achieved) SlColor.Dark.GreenBorder else SlColor.Dark.Border
    val iconTint = if (state == ChipState.Achieved) {
        SlColor.Dark.GreenIcon
    } else {
        SlColor.Dark.TextMuted
    }
    val textColor = if (state == ChipState.Achieved) SlColor.Dark.GreenText else SlColor.Dark.TextChip

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(container)
            .border(1.dp, border, CircleShape)
            .padding(horizontal = 13.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(14.dp),
        )
        Text(text = text, style = SlText.Chip, color = textColor)
    }
}
