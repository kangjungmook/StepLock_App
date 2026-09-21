package com.steplock.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.steplock.app.ui.theme.SlColor
import com.steplock.app.ui.theme.SlDimen
import com.steplock.app.ui.theme.SlText
import com.steplock.app.ui.theme.StepLockTheme

/**
 * 되돌릴 수 없는 동작을 한 번 더 묻습니다.
 *
 * Material3 기본 대화상자는 팔레트가 따로 돌아가서, 앱 토큰으로 직접 그렸습니다.
 * 확인 버튼은 무엇이 일어나는지 적고(‘삭제’가 아니라 ‘계정과 기록 삭제하기’),
 * 취소 쪽을 밑줄 없는 링크로 둬서 실수로 누르기 어렵게 했습니다.
 */
@Composable
fun SlConfirmDialog(
    title: String,
    description: String,
    confirmText: String,
    cancelText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    errorText: String? = null,
    busy: Boolean = false,
    confirmContainerColor: Color = SlColor.Error,
    confirmContentColor: Color = SlColor.OnError,
) {
    Dialog(onDismissRequest = { if (!busy) onDismiss() }) {
        Column(
            modifier = modifier
                .widthIn(max = 340.dp)
                .clip(RoundedCornerShape(SlDimen.RadiusPanel))
                .background(SlColor.Surface)
                .padding(24.dp),
        ) {
            Text(text = title, style = SlText.DialogTitle, color = SlColor.TextPrimary)
            Spacer(Modifier.height(8.dp))
            Text(text = description, style = SlText.Body, color = SlColor.TextSecondary)

            if (errorText != null) {
                Spacer(Modifier.height(12.dp))
                Text(text = errorText, style = SlText.Caption, color = SlColor.Error)
            }

            Spacer(Modifier.height(24.dp))
            PrimaryButton(
                text = confirmText,
                onClick = onConfirm,
                enabled = !busy,
                containerColor = confirmContainerColor,
                contentColor = confirmContentColor,
            )
            TextLink(
                text = cancelText,
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                style = SlText.LinkSm,
                color = SlColor.TextSecondary,
                underline = false,
            )
        }
    }
}

@Preview
@Composable
private fun SlConfirmDialogPreview() {
    StepLockTheme {
        SlConfirmDialog(
            title = "계정과 기록을 모두 삭제할까요?",
            description = "계정, 서버에 저장된 설정과 일별 기록, 이 기기에 남은 기록이 " +
                "모두 지워집니다. 되돌릴 수 없어요.",
            confirmText = "계정과 기록 삭제하기",
            cancelText = "그대로 두기",
            onConfirm = {},
            onDismiss = {},
        )
    }
}
