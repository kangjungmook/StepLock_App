package com.steplock.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.steplock.app.R
import com.steplock.app.system.PermissionGroup
import com.steplock.app.ui.components.CheckboxMark
import com.steplock.app.ui.components.IconTile
import com.steplock.app.ui.components.PrimaryButton
import com.steplock.app.ui.components.SectionLabel
import com.steplock.app.ui.components.SlDetailRow
import com.steplock.app.ui.components.SlDivider
import com.steplock.app.ui.components.SlIcons
import com.steplock.app.ui.components.SlPanel
import com.steplock.app.ui.components.StepLockMascot
import com.steplock.app.ui.components.TextLink
import com.steplock.app.ui.theme.SlColor
import com.steplock.app.ui.theme.SlDimen
import com.steplock.app.ui.theme.SlText
import com.steplock.app.ui.theme.StepLockTheme

/**
 * 첫 화면. 앱이 무엇을 하는지 한 줄로 말하고, **필요한 허용 세 줄을 한 화면에**
 * 보여 줍니다.
 *
 * 전에는 버튼 하나가 남은 권한을 하나씩만 요구해서, 누를 때마다 버튼 뜻이 바뀌고
 * 몇 개가 남았는지 알 수 없었습니다. 이제 세 줄이 같이 보이고 각 줄에서 바로
 * 허용하며, 끝난 줄에는 체크가 붙습니다.
 */
@Composable
fun OnboardingScreen(
    /** 권한별 허용 여부. 순서대로 화면에 나열됩니다. */
    permissions: List<Pair<PermissionGroup, Boolean>>,
    onPermissionClick: (PermissionGroup) -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val remaining = permissions.count { !it.second }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SlColor.Background)
            .safeDrawingPadding(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = SlDimen.ScreenPaddingWide,
                    end = SlDimen.ScreenPaddingWide,
                    top = 28.dp,
                ),
        ) {
            StepLockMascot(modifier = Modifier.size(width = 96.dp, height = 119.dp))

            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = SlText.AppTitle,
                color = SlColor.TextPrimary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.onboarding_tagline),
                style = SlText.Tagline,
                color = SlColor.TextSecondary,
            )

            Spacer(Modifier.height(28.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SectionLabel(
                    text = stringResource(R.string.onboarding_permissions_label),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    // 몇 개가 남았는지 숫자로 알려 줍니다 — 끝이 보이지 않으면
                    // 설정 화면을 오갈 이유를 납득하기 어렵습니다.
                    text = if (remaining == 0) {
                        stringResource(R.string.onboarding_permissions_done)
                    } else {
                        stringResource(R.string.onboarding_permissions_left, remaining)
                    },
                    style = SlText.LabelSm,
                    color = if (remaining == 0) SlColor.BrandDeep else SlColor.AmberText,
                )
            }

            Spacer(Modifier.height(12.dp))
            SlPanel {
                permissions.forEachIndexed { index, (group, granted) ->
                    if (index > 0) SlDivider()
                    PermissionRow(
                        group = group,
                        granted = granted,
                        onClick = { onPermissionClick(group) },
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.onboarding_permissions_note),
                style = SlText.Caption,
                color = SlColor.TextTertiary,
            )

            Spacer(Modifier.height(28.dp))
            SectionLabel(stringResource(R.string.onboarding_caption))
            Spacer(Modifier.height(12.dp))
            SlPanel(
                shape = RoundedCornerShape(SlDimen.RadiusPanel),
                contentPadding = PaddingValues(20.dp),
            ) {
                UnlockRule(
                    icon = SlIcons.Steps,
                    title = stringResource(R.string.onboarding_steps_title),
                    description = stringResource(R.string.onboarding_steps_desc),
                )
                SlDivider(modifier = Modifier.padding(vertical = 16.dp))
                UnlockRule(
                    icon = SlIcons.Moon,
                    title = stringResource(R.string.onboarding_sleep_title),
                    description = stringResource(R.string.onboarding_sleep_desc),
                )
                SlDivider(modifier = Modifier.padding(vertical = 16.dp))
                UnlockRule(
                    icon = SlIcons.Timer,
                    title = stringResource(R.string.onboarding_pomodoro_title),
                    description = stringResource(R.string.onboarding_pomodoro_desc),
                )
            }
            Spacer(Modifier.height(24.dp))
        }

        Column(
            modifier = Modifier.padding(
                start = SlDimen.ScreenPaddingWide,
                end = SlDimen.ScreenPaddingWide,
                bottom = 24.dp,
            ),
        ) {
            PrimaryButton(
                // 남은 게 있으면 다음 필요한 것을 열어 줍니다. 목록에서 직접 눌러도
                // 되지만, 아래 버튼만 계속 눌러도 끝까지 갈 수 있어야 합니다.
                text = if (remaining == 0) {
                    stringResource(R.string.permission_cta_start)
                } else {
                    stringResource(R.string.permission_cta_remaining)
                },
                onClick = {
                    if (remaining == 0) {
                        onStart()
                    } else {
                        permissions.first { !it.second }.let { onPermissionClick(it.first) }
                    }
                },
            )
            // 남은 권한이 있어도 들어갈 수 있어야 합니다. 이 길이 없으면, 런타임
            // 권한을 두 번 거절해 안드로이드가 대화상자를 더 띄우지 않는 사용자는
            // **이 화면에서 영구히 막혀 앱을 아예 쓸 수 없습니다.** 권한 없이 들어가면
            // 홈이 무엇이 꺼져 있는지 알려 주고 해당 설정 화면으로 보내 줍니다.
            if (remaining > 0) {
                Spacer(Modifier.height(4.dp))
                TextLink(
                    text = stringResource(R.string.onboarding_skip_permissions),
                    onClick = onStart,
                    underline = false,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.onboarding_privacy),
                style = SlText.Caption,
                color = SlColor.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** 권한 한 줄 — 허용되면 체크로 바뀌고 더 이상 눌리지 않습니다. */
@Composable
private fun PermissionRow(group: PermissionGroup, granted: Boolean, onClick: () -> Unit) {
    SlDetailRow(
        title = stringResource(group.titleRes),
        description = stringResource(group.descRes),
        modifier = Modifier
            .then(if (granted) Modifier else Modifier.clickable(role = Role.Button, onClick = onClick))
            .padding(vertical = 14.dp),
        trailing = {
            if (granted) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CheckboxMark(checked = true)
                    Text(
                        text = stringResource(R.string.permission_granted),
                        style = SlText.LabelSm,
                        color = SlColor.BrandDeep,
                    )
                }
            } else {
                Text(
                    text = stringResource(
                        if (group.opensSettings) {
                            R.string.permission_action_open
                        } else {
                            R.string.permission_action_allow
                        },
                    ),
                    style = SlText.Chip,
                    color = SlColor.BrandDeep,
                )
            }
        },
    )
}

@Composable
private fun UnlockRule(icon: ImageVector, title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        IconTile(
            icon = icon,
            tint = SlColor.BrandDeep,
            background = SlColor.SurfaceAlt,
            size = SlDimen.TouchTarget,
            shape = RoundedCornerShape(SlDimen.RadiusField),
            iconSize = 22.dp,
        )
        Column {
            Text(text = title, style = SlText.RowTitle, color = SlColor.TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text(text = description, style = SlText.BodySm, color = SlColor.TextSecondary)
        }
    }
}

@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun OnboardingScreenPreview() {
    StepLockTheme {
        OnboardingScreen(
            permissions = listOf(
                PermissionGroup.Runtime to true,
                PermissionGroup.UsageAccess to false,
                PermissionGroup.Overlay to false,
            ),
            onPermissionClick = {},
            onStart = {},
        )
    }
}

/** 세 개를 다 허용해 시작 버튼이 열린 상태. */
@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun OnboardingScreenReadyPreview() {
    StepLockTheme {
        OnboardingScreen(
            permissions = PermissionGroup.entries.map { it to true },
            onPermissionClick = {},
            onStart = {},
        )
    }
}
