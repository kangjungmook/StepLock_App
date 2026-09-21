package com.steplock.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.steplock.app.ui.theme.SlColor
import com.steplock.app.ui.theme.SlDimen
import com.steplock.app.ui.theme.SlText

/** 조건 카드 — 아이콘 + 제목 + 스위치, 구분선 아래 목표값 스테퍼. */
@Composable
fun ConditionSettingCard(
    icon: ImageVector,
    title: String,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    toggleLabel: String,
    goalLabel: String,
    goalValue: String,
    decreaseLabel: String,
    increaseLabel: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(SlDimen.RadiusCard)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SlColor.Surface)
            .border(1.dp, SlColor.Border, shape)
            .padding(SlDimen.PanelPadding),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTile(
                icon = icon,
                tint = SlColor.BrandDeep,
                background = SlColor.SurfaceAlt,
                size = 40.dp,
                shape = RoundedCornerShape(SlDimen.RadiusBadge),
                iconSize = 20.dp,
            )
            Text(
                text = title,
                style = SlText.RowTitle,
                color = SlColor.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            SlSwitch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
                contentDescription = toggleLabel,
            )
        }
        SlDivider(modifier = Modifier.padding(vertical = 16.dp))
        GoalStepper(
            label = goalLabel,
            value = goalValue,
            decreaseLabel = decreaseLabel,
            increaseLabel = increaseLabel,
            onDecrease = onDecrease,
            onIncrease = onIncrease,
        )
    }
}

@Composable
fun GoalStepper(
    label: String,
    value: String,
    decreaseLabel: String,
    increaseLabel: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = SlText.StepperLabel,
            color = SlColor.TextSecondary,
            modifier = Modifier.weight(1f),
        )
        StepperButton(icon = SlIcons.Minus, contentDescription = decreaseLabel, onClick = onDecrease)
        Text(
            text = value,
            style = SlText.StepperValue,
            color = SlColor.TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 88.dp),
        )
        StepperButton(icon = SlIcons.Plus, contentDescription = increaseLabel, onClick = onIncrease)
    }
}

@Composable
private fun StepperButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(SlDimen.RadiusField)
    Box(
        modifier = Modifier
            .size(SlDimen.TouchTarget)
            .clip(shape)
            .background(SlColor.Background)
            .border(1.dp, SlColor.Border, shape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = SlColor.TextPrimary,
            modifier = Modifier.size(18.dp),
        )
    }
}
