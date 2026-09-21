package com.steplock.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.steplock.app.ui.theme.SlColor
import com.steplock.app.ui.theme.SlDimen
import com.steplock.app.ui.theme.SlText

fun appBadgeColor(appId: String): Color = when (appId) {
    "shorts" -> SlColor.AppBadge.Shorts
    "reels" -> SlColor.AppBadge.Reels
    "tiktok" -> SlColor.AppBadge.TikTok
    else -> SlColor.AppBadge.X
}

/** 앱 뱃지 + 이름(부제) + 뒤쪽 상태 슬롯. 홈의 상태 점과 설정의 체크박스가 같은 행을 씁니다. */
@Composable
fun AppListItem(
    name: String,
    badgeInitial: String,
    badgeColor: Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    badgeSize: Dp = 40.dp,
    nameStyle: TextStyle = SlText.RowTitle,
    verticalPadding: Dp = 14.dp,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = SlDimen.TouchTarget)
            .padding(vertical = verticalPadding),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppBadge(initial = badgeInitial, color = badgeColor, size = badgeSize)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, style = nameStyle, color = SlColor.TextPrimary)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = SlText.AppSubtitle,
                    color = SlColor.TextSecondary,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
        trailing()
    }
}
