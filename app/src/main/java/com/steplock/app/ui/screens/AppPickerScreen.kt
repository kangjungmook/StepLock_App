package com.steplock.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.steplock.app.R
import com.steplock.app.data.InstalledApp
import com.steplock.app.ui.components.AppIcon
import com.steplock.app.ui.components.CheckboxMark
import com.steplock.app.ui.components.IconTapTarget
import com.steplock.app.ui.components.SectionLabel
import com.steplock.app.ui.components.SlDivider
import com.steplock.app.ui.components.SlEmptyState
import com.steplock.app.ui.components.SlIcons
import com.steplock.app.ui.components.SlPanel
import com.steplock.app.ui.components.UnderlineTextField
import com.steplock.app.ui.theme.SlColor
import com.steplock.app.ui.theme.SlDimen
import com.steplock.app.ui.theme.SlText
import com.steplock.app.ui.theme.StepLockTheme

/**
 * 기기에 깔린 앱에서 잠글 앱을 고릅니다.
 *
 * 전에는 쇼츠·릴스·틱톡·엑스 네 개만 목록에 있었고 패키지 이름이 코드에 박혀
 * 있었습니다. 그래서 **틱톡 라이트처럼 패키지가 다른 앱은 골라도 걸리지 않았습니다.**
 *
 * 고른 앱을 맨 위로 올립니다 — 수십 개짜리 목록에서 지금 뭘 골랐는지 확인하려고
 * 끝까지 내려야 하면 안 됩니다. 검색은 이름과 패키지 이름을 함께 봅니다.
 */
@Composable
fun AppPickerScreen(
    apps: List<InstalledApp>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }

    val visible = remember(apps, selected, query) {
        val keyword = query.trim()
        apps
            .filter {
                keyword.isEmpty() ||
                    it.label.contains(keyword, ignoreCase = true) ||
                    it.packageName.contains(keyword, ignoreCase = true)
            }
            // 고른 앱 먼저, 그다음 이름 순.
            .sortedWith(compareByDescending<InstalledApp> { it.packageName in selected }.thenBy { it.label })
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SlColor.Background)
            .safeDrawingPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTapTarget(
                icon = SlIcons.ArrowLeft,
                contentDescription = stringResource(R.string.action_back),
                onClick = onBack,
            )
            Text(
                text = stringResource(R.string.app_picker_title),
                style = SlText.ScreenTitle,
                color = SlColor.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.app_picker_selected, selected.size),
                style = SlText.LabelSm,
                color = if (selected.isEmpty()) SlColor.TextSecondary else SlColor.BrandDeep,
                modifier = Modifier.padding(end = 8.dp),
            )
        }

        Column(modifier = Modifier.padding(horizontal = SlDimen.ScreenPadding)) {
            UnderlineTextField(
                value = query,
                onValueChange = { query = it },
                label = stringResource(R.string.app_picker_search_label),
                placeholder = stringResource(R.string.app_picker_search_hint),
                leadingIcon = SlIcons.Search,
                imeAction = ImeAction.Search,
            )
            Spacer(Modifier.height(20.dp))
        }

        if (visible.isEmpty()) {
            Column(modifier = Modifier.padding(horizontal = SlDimen.ScreenPadding)) {
                SlPanel {
                    SlEmptyState(
                        title = stringResource(
                            if (apps.isEmpty()) {
                                R.string.app_picker_none_title
                            } else {
                                R.string.app_picker_no_match_title
                            },
                        ),
                        description = stringResource(
                            if (apps.isEmpty()) {
                                R.string.app_picker_none_desc
                            } else {
                                R.string.app_picker_no_match_desc
                            },
                        ),
                    )
                }
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = SlDimen.ScreenPadding,
                end = SlDimen.ScreenPadding,
                bottom = 24.dp,
            ),
        ) {
            item {
                SectionLabel(
                    text = stringResource(R.string.app_picker_hint),
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            items(visible, key = { it.packageName }) { app ->
                val checked = app.packageName in selected
                AppPickerRow(
                    app = app,
                    checked = checked,
                    onToggle = { onToggle(app.packageName) },
                )
                SlDivider()
            }
        }
    }
}

/** 앱 한 줄. 아이콘 없이는 "라이트" 같은 변종을 구분할 수 없어 아이콘을 함께 씁니다. */
@Composable
private fun AppPickerRow(app: InstalledApp, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() })
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(packageName = app.packageName, label = app.label, size = 40.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = app.label, style = SlText.ListItem, color = SlColor.TextPrimary)
            Text(
                text = app.packageName,
                style = SlText.LabelSm,
                color = SlColor.TextTertiary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        CheckboxMark(checked = checked)
    }
}

@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun AppPickerScreenPreview() {
    StepLockTheme {
        AppPickerScreen(
            apps = listOf(
                InstalledApp("com.ss.android.ugc.tiktok.lite", "TikTok Lite"),
                InstalledApp("com.google.android.youtube", "YouTube"),
                InstalledApp("com.instagram.android", "Instagram"),
                InstalledApp("com.kakao.talk", "카카오톡"),
                InstalledApp("com.nhn.android.search", "네이버"),
            ),
            selected = setOf("com.ss.android.ugc.tiktok.lite", "com.google.android.youtube"),
            onToggle = {},
            onBack = {},
        )
    }
}
