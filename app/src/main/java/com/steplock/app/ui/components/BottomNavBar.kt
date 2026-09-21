package com.steplock.app.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.steplock.app.R
import com.steplock.app.ui.theme.SlColor
import com.steplock.app.ui.theme.SlText

enum class NavTab(val icon: ImageVector, @StringRes val labelRes: Int) {
    Home(SlIcons.Home, R.string.nav_home),
    Stats(SlIcons.Stats, R.string.nav_stats),
    Settings(SlIcons.Settings, R.string.nav_settings),
}

@Composable
fun BottomNavBar(
    selected: NavTab,
    onSelect: (NavTab) -> Unit,
    modifier: Modifier = Modifier,
    showLabels: Boolean = true,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SlColor.Surface)
            .navigationBarsPadding(),
    ) {
        SlDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp),
        ) {
            NavTab.entries.forEach { tab ->
                val label = stringResource(tab.labelRes)
                val active = tab == selected
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 52.dp)
                        .clickable(
                            role = Role.Tab,
                            onClickLabel = label,
                            onClick = { onSelect(tab) },
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = if (showLabels) null else label,
                        tint = if (active) SlColor.BrandDeep else SlColor.TextSecondary,
                        modifier = Modifier.size(22.dp),
                    )
                    if (showLabels) {
                        Text(
                            text = label,
                            style = if (active) SlText.NavLabelActive else SlText.NavLabel,
                            color = if (active) SlColor.BrandDeep else SlColor.TextSecondary,
                        )
                    }
                }
            }
        }
    }
}
