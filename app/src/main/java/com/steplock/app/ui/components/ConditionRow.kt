package com.steplock.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.steplock.app.ui.theme.SlColor
import com.steplock.app.ui.theme.SlText

/** 홈의 잠금 해제 조건 한 줄 — 앞쪽 시각 요소와 뒤쪽 상태를 슬롯으로 받습니다. */
@Composable
fun ConditionRow(
    title: String,
    value: String,
    leading: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = SlText.RowTitle, color = SlColor.TextPrimary)
            Text(
                text = value,
                style = SlText.RowValue,
                color = SlColor.TextSecondary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        trailing?.invoke()
    }
}
