package com.steplock.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.steplock.app.R
import com.steplock.app.system.PermissionGroup
import com.steplock.app.ui.components.MascotMood
import com.steplock.app.ui.components.PrimaryButton
import com.steplock.app.ui.components.SlIcons
import com.steplock.app.ui.components.StepLockMascot
import com.steplock.app.ui.components.TextLink
import com.steplock.app.ui.components.Wordmark
import com.steplock.app.ui.theme.SlColor
import com.steplock.app.ui.theme.SlDimen
import com.steplock.app.ui.theme.SlText
import com.steplock.app.ui.theme.StepLockTheme

/**
 * 첫 화면 — 허용 세 개를 **번호 붙은 세 걸음**으로.
 *
 * 패널 안에 같은 무게로 늘어선 세 줄은 "어디부터 하지?"를 남겼습니다. 이제 왼쪽에
 * 1 · 2 · 3 표식이 세로 길로 이어지고, **지금 할 차례 하나만** 펼쳐져 설명과 버튼을
 * 보여 줍니다. 끝난 걸음은 흑연색 체크로 접히고, 다음 걸음은 번호만 남습니다.
 * 위에서부터 차례대로 누르기만 하면 끝까지 갑니다.
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
    val currentIndex = permissions.indexOfFirst { !it.second }

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
                .padding(start = SlDimen.ScreenPadding, end = SlDimen.ScreenPadding, top = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Wordmark(text = stringResource(R.string.app_wordmark), modifier = Modifier.weight(1f))
                Text(
                    // 몇 개가 남았는지 숫자로 — 끝이 보여야 설정 화면을 오갈 이유가 납득됩니다.
                    text = if (remaining == 0) {
                        stringResource(R.string.onboarding_permissions_done)
                    } else {
                        stringResource(R.string.onboarding_permissions_left, remaining)
                    },
                    style = SlText.LabelSm,
                    color = SlColor.TextSecondary,
                )
            }

            Spacer(Modifier.height(32.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = stringResource(
                        if (remaining == 0) R.string.onboarding_headline_done else R.string.onboarding_headline,
                    ),
                    style = SlText.HomeBig,
                    color = SlColor.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                StepLockMascot(
                    modifier = Modifier.size(width = 56.dp, height = 69.dp),
                    mood = if (remaining == 0) MascotMood.Resting else MascotMood.Walking,
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.onboarding_tagline),
                style = SlText.Tagline,
                color = SlColor.TextSecondary,
            )

            Spacer(Modifier.height(40.dp))
            permissions.forEachIndexed { index, (group, granted) ->
                PermissionStep(
                    number = index + 1,
                    group = group,
                    state = when {
                        granted -> StepState.Done
                        index == currentIndex -> StepState.Current
                        else -> StepState.Todo
                    },
                    isLast = index == permissions.lastIndex,
                    onClick = { onPermissionClick(group) },
                )
            }
            Spacer(Modifier.height(24.dp))
        }

        Column(
            modifier = Modifier.padding(
                start = SlDimen.ScreenPadding,
                end = SlDimen.ScreenPadding,
                bottom = 24.dp,
            ),
        ) {
            if (remaining == 0) {
                PrimaryButton(text = stringResource(R.string.permission_cta_start), onClick = onStart)
            } else {
                // 남은 권한이 있어도 들어갈 수 있어야 합니다. 이 길이 없으면, 런타임
                // 권한을 두 번 거절해 안드로이드가 대화상자를 더 띄우지 않는 사용자는
                // **이 화면에서 영구히 막혀 앱을 아예 쓸 수 없습니다.** 권한 없이 들어가면
                // 홈이 무엇이 꺼져 있는지 알려 주고 해당 설정 화면으로 보내 줍니다.
                TextLink(
                    text = stringResource(R.string.onboarding_skip_permissions),
                    onClick = onStart,
                    underline = false,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.onboarding_privacy),
                style = SlText.Caption,
                color = SlColor.TextTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private enum class StepState { Done, Current, Todo }

/**
 * 한 걸음. 왼쪽 표식과 아래로 이어지는 선, 오른쪽 내용.
 *
 * 지금 차례인 걸음만 설명 · (설정 화면에서 켜는 경우) 무엇을 누르면 되는지 ·
 * 버튼까지 펼칩니다. 설정 화면은 기기마다 생김새가 달라서, 무엇을 찾아야 하는지
 * 모르면 그 화면에서 길을 잃습니다.
 */
@Composable
private fun PermissionStep(
    number: Int,
    group: PermissionGroup,
    state: StepState,
    isLast: Boolean,
    onClick: () -> Unit,
) {
    val guideRes = when (group) {
        PermissionGroup.Overlay -> R.string.permission_overlay_steps
        PermissionGroup.UsageAccess -> R.string.permission_usage_steps
        PermissionGroup.Runtime -> null
    }
    val ctaRes = when (group) {
        PermissionGroup.Runtime -> R.string.permission_cta_activity
        PermissionGroup.Overlay -> R.string.permission_cta_overlay
        PermissionGroup.UsageAccess -> R.string.permission_cta_usage
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .then(
                // 차례가 아닌 걸음도 눌러서 먼저 켤 수 있습니다.
                if (state == StepState.Todo) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier,
            ),
    ) {
        Column(
            modifier = Modifier.width(MARK_SIZE).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StepMark(number = number, state = state)
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .width(2.dp)
                        .weight(1f)
                        .background(if (state == StepState.Done) SlColor.TextPrimary else SlColor.Border),
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 28.dp),
        ) {
            Row(
                modifier = Modifier.heightIn(min = MARK_SIZE),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(group.titleRes),
                    style = SlText.RowTitle.copy(fontSize = 17.sp, lineHeight = 24.sp),
                    color = if (state == StepState.Todo) SlColor.TextSecondary else SlColor.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                if (state == StepState.Done) {
                    Text(
                        text = stringResource(R.string.permission_granted),
                        style = SlText.LabelSm,
                        color = SlColor.TextSecondary,
                    )
                }
            }
            Text(
                text = stringResource(group.descRes),
                style = SlText.BodySm,
                color = SlColor.TextSecondary,
            )
            if (state == StepState.Current) {
                if (guideRes != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(guideRes),
                        style = SlText.Caption,
                        color = SlColor.TextSecondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SlColor.SurfaceAlt, RoundedCornerShape(SlDimen.RadiusSmall))
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
                PrimaryButton(text = stringResource(ctaRes), onClick = onClick, height = 52.dp)
            }
        }
    }
}

/** 걸음 표식 — 끝남은 흑연 체크, 지금은 주황 번호, 남음은 테두리 번호. */
@Composable
private fun StepMark(number: Int, state: StepState) {
    val shape = RoundedCornerShape(SlDimen.RadiusSmall)
    Box(
        modifier = Modifier
            .size(MARK_SIZE)
            .clip(shape)
            .then(
                when (state) {
                    StepState.Done -> Modifier.background(SlColor.TextPrimary)
                    StepState.Current -> Modifier.background(SlColor.Brand)
                    StepState.Todo -> Modifier.border(1.5.dp, SlColor.BorderStrong, shape)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (state == StepState.Done) {
            Icon(
                imageVector = SlIcons.CheckBold,
                contentDescription = null,
                tint = SlColor.Background,
                modifier = Modifier.size(18.dp),
            )
        } else {
            Text(
                text = number.toString(),
                style = SlText.RowNumeral.copy(fontSize = 20.sp, lineHeight = 20.sp),
                color = if (state == StepState.Current) SlColor.OnBrand else SlColor.TextTertiary,
            )
        }
    }
}

private val MARK_SIZE = 32.dp

@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun OnboardingScreenPreview() {
    StepLockTheme {
        OnboardingScreen(
            permissions = listOf(
                PermissionGroup.Runtime to true,
                PermissionGroup.Overlay to false,
                PermissionGroup.UsageAccess to false,
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
