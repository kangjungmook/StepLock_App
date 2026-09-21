package com.steplock.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.steplock.app.ui.theme.SlColor
import com.steplock.app.ui.theme.SlDimen
import com.steplock.app.ui.theme.SlText

/** 앰버 톤 잠금 상태 배너 — 탭하면 잠금 설정으로 이동합니다. */
@Composable
fun LockBanner(
    title: String,
    subtitle: String,
    actionLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(SlDimen.RadiusCard)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SlColor.AmberSurface)
            .border(1.dp, SlColor.AmberBorder, shape)
            .clickable(role = Role.Button, onClickLabel = actionLabel, onClick = onClick)
            .padding(SlDimen.PanelPadding),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(
            icon = SlIcons.Lock,
            tint = SlColor.OnAmber,
            background = SlColor.Amber,
            size = 40.dp,
            shape = RoundedCornerShape(SlDimen.RadiusBadge),
            iconSize = 20.dp,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = SlText.RowTitle, color = SlColor.AmberText)
            Text(
                text = subtitle,
                style = SlText.RowValue,
                color = SlColor.AmberSubText,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        Icon(
            imageVector = SlIcons.ChevronRight,
            contentDescription = null,
            tint = SlColor.AmberSubText,
            modifier = Modifier.size(20.dp),
        )
    }
}
