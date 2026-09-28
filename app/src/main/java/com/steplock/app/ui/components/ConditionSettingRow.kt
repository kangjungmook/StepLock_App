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

/**
 * 조건 한 줄 — 아이콘 + 제목 + 스위치, 켜져 있으면 아래에 목표값 스테퍼.
 *
 * 예전에는 조건마다 카드를 따로 두어서 설정 화면에 카드가 일곱 장 쌓였습니다.
 * 같은 종류의 항목 셋은 **패널 하나 안에서 구분선으로** 나누고, 섹션 사이는
 * 여백으로 나눕니다. 꺼진 조건의 목표값은 숨깁니다 — 쓰지 않는 조건의 스테퍼가
 * 보이면 켜 둔 것처럼 읽히고, 화면 길이만 늘어납니다.
 */
@Composable
fun ConditionSettingRow(
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
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTile(
                icon = icon,
                // 꺼진 조건은 아이콘도 한 단계 가라앉힙니다 — 스위치만 보고
                // 판단하지 않아도 무엇이 켜져 있는지 훑어 읽힙니다.
                tint = if (enabled) SlColor.BrandDeep else SlColor.TextTertiary,
                background = if (enabled) SlColor.BrandTintAlt else SlColor.SurfaceAlt,
                size = 40.dp,
                shape = RoundedCornerShape(SlDimen.RadiusBadge),
                iconSize = 20.dp,
            )
            Text(
                text = title,
                style = SlText.RowTitle,
                color = if (enabled) SlColor.TextPrimary else SlColor.TextSecondary,
                modifier = Modifier.weight(1f),
            )
            SlSwitch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
                contentDescription = toggleLabel,
            )
        }
        if (enabled) {
            GoalStepper(
                label = goalLabel,
                value = goalValue,
                decreaseLabel = decreaseLabel,
                increaseLabel = increaseLabel,
                onDecrease = onDecrease,
                onIncrease = onIncrease,
                // 아이콘 폭(40) + 간격(14)만큼 들여 제목과 같은 선에서 시작합니다.
                modifier = Modifier.padding(start = 54.dp, top = 12.dp),
            )
        }
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
