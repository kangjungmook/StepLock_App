package com.steplock.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.steplock.app.ui.theme.StepLockTheme

/**
 * 보여 줄 내용이 없는 패널 안에 들어갑니다.
 *
 * 빈 면을 그대로 두면 고장처럼 보이므로, 무엇이 없는지와 다음에 무엇을 하면
 * 되는지를 같이 적습니다. `onClick` 을 주면 셰브론이 붙고 줄 전체가 눌립니다 —
 * 사용자가 직접 해결할 수 있을 때만 주고, 그냥 기다려야 하는 경우에는 비웁니다.
 */
@Composable
fun SlEmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    SlDetailRow(
        title = title,
        description = description,
        modifier = modifier
            .then(
                if (onClick != null) {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(vertical = 16.dp),
        trailing = if (onClick != null) {
            { SlChevron() }
        } else {
            null
        },
    )
}

@Preview
@Composable
private fun SlEmptyStatePreview() {
    StepLockTheme {
        SlPanel {
            SlEmptyState(
                title = "켜 둔 조건이 없어요",
                description = "설정에서 조건을 켜면 잠금이 시작돼요",
                onClick = {},
            )
        }
    }
}
