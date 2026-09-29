package com.steplock.app.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.steplock.app.R
import com.steplock.app.ui.theme.SlColor
import com.steplock.app.ui.theme.SlDimen
import com.steplock.app.ui.theme.SlText
import com.steplock.app.ui.theme.StepLockTheme

enum class NavTab(val icon: ImageVector, @StringRes val labelRes: Int) {
    Home(SlIcons.Home, R.string.nav_home),
    Stats(SlIcons.Stats, R.string.nav_stats),
    Settings(SlIcons.Settings, R.string.nav_settings),
}

/**
 * 화면 아래에 **떠 있는** 알약 모양 탭.
 *
 * 화면 끝에 붙은 막대 대신 좌우 24dp · 아래 12dp 를 띄우고 그림자를 약하게 줍니다.
 * 내용이 탭 뒤로 흘러가도록, 쓰는 화면은 이 탭을 내용 위에 겹쳐 두고 스크롤 끝에
 * [SlDimen.FloatingNavReserve] 만큼 여백을 둡니다.
 *
 * 이름은 셋 다 보입니다 — 아이콘만으로는 "통계"와 "설정"을 처음 보는 사람이
 * 바로 알아보기 어렵습니다. 지금 탭은 연한 브랜드 알약으로 감쌉니다.
 */
@Composable
fun BottomNavBar(
    selected: NavTab,
    onSelect: (NavTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = CircleShape
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, bottom = 12.dp)
            // 그림자는 넓게 번지게, 색은 배경 색조를 따라 아주 옅게 — 떠 있다는 것만 알립니다.
            .shadow(
                elevation = 12.dp,
                shape = shape,
                ambientColor = SlColor.Shadow,
                spotColor = SlColor.Shadow,
            )
            .clip(shape)
            .background(SlColor.Surface)
            .border(1.dp, SlColor.Border, shape)
            .height(NAV_HEIGHT)
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavTab.entries.forEach { tab ->
            val active = tab == selected
            val label = stringResource(tab.labelRes)
            val tint = if (active) SlColor.BrandDeep else SlColor.TextSecondary
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(shape)
                    .background(if (active) SlColor.BrandTintAlt else Color.Transparent)
                    .selectable(
                        selected = active,
                        role = Role.Tab,
                        onClick = { onSelect(tab) },
                    ),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = label,
                    style = if (active) SlText.NavLabelActive else SlText.NavLabel,
                    color = tint,
                )
            }
        }
    }
}

/** 알약 높이. 안쪽 탭 하나는 52dp — 44dp 터치 기준을 넉넉히 넘깁니다. */
private val NAV_HEIGHT = 64.dp

@Preview(widthDp = 412)
@Composable
private fun BottomNavBarPreview() {
    StepLockTheme {
        BottomNavBar(selected = NavTab.Home, onSelect = {})
    }
}
