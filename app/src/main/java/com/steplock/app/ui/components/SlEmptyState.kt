package com.steplock.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
 *
 * 글자 왼쪽에 캐릭터를 작게 둡니다. 빈 화면은 캐릭터가 가장 값을 하는 자리입니다 —
 * 아무것도 없으면 망가진 화면처럼 보이지만, 캐릭터가 서 있으면 "아직 시작하지
 * 않았을 뿐"으로 읽힙니다. 걷지 않는 [MascotMood.Resting] 을 씁니다: 여기서
 * 움직이면 읽어야 할 안내보다 캐릭터가 먼저 눈에 들어옵니다.
 */
@Composable
fun SlEmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepLockMascot(
            modifier = Modifier.size(width = 36.dp, height = 45.dp),
            mood = MascotMood.Resting,
        )
        SlDetailRow(
            title = title,
            description = description,
            modifier = Modifier.weight(1f),
            trailing = if (onClick != null) {
                { SlChevron() }
            } else {
                null
            },
        )
    }
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
