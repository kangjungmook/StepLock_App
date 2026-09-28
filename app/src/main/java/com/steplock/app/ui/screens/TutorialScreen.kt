package com.steplock.app.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.steplock.app.R
import com.steplock.app.data.TemporaryAllow
import com.steplock.app.ui.components.MascotMood
import com.steplock.app.ui.components.PrimaryButton
import com.steplock.app.ui.components.SlIcons
import com.steplock.app.ui.components.StepLockMascot
import com.steplock.app.ui.components.StepiBubble
import com.steplock.app.ui.components.TextLink
import com.steplock.app.ui.theme.SlColor
import com.steplock.app.ui.theme.SlDimen
import com.steplock.app.ui.theme.SlText
import com.steplock.app.ui.theme.StepLockTheme
import kotlinx.coroutines.launch

/** 튜토리얼 한 장. 캐릭터가 말풍선으로 한 가지씩만 말합니다. */
private data class TutorialPage(
    @StringRes val titleRes: Int,
    @StringRes val bodyRes: Int,
    val mood: MascotMood,
    /** 말풍선 아래에 늘어놓을 조건 아이콘. 조건을 설명하는 장에서만 씁니다. */
    val showConditions: Boolean = false,
)

private val pages = listOf(
    TutorialPage(R.string.tutorial_hello_title, R.string.tutorial_hello_body, MascotMood.Walking),
    TutorialPage(
        R.string.tutorial_unlock_title,
        R.string.tutorial_unlock_body,
        MascotMood.Walking,
        showConditions = true,
    ),
    TutorialPage(R.string.tutorial_allow_title, R.string.tutorial_allow_body, MascotMood.Resting),
    TutorialPage(R.string.tutorial_ready_title, R.string.tutorial_ready_body, MascotMood.Focusing),
)

/**
 * 첫 실행 튜토리얼. 캐릭터가 말풍선으로 앱이 하는 일을 네 장에 나눠 알려 줍니다.
 *
 * 권한 화면보다 **먼저** 둡니다. 무엇을 하는 앱인지 모르는 채로 "사용 정보 접근"
 * 같은 권한을 요구받으면 거절하기 쉽고, 거절하면 앱이 아무것도 못 합니다.
 *
 * 한 장에 한 가지만 말합니다. 글이 길면 넘기기만 하고 읽지 않습니다.
 * 넘기는 건 스와이프와 아래 버튼 둘 다 되고, 언제든 건너뛸 수 있습니다.
 *
 * @param replay 설정에서 다시 보는 경우. 마지막 버튼이 권한 화면 대신 닫기가 됩니다.
 */
@Composable
fun TutorialScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    replay: Boolean = false,
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == pages.lastIndex

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SlColor.Background)
            .safeDrawingPadding(),
    ) {
        // 건너뛰기는 마지막 장에서 사라집니다 — 거기서는 아래 버튼이 같은 일을 합니다.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = SlDimen.TouchTarget)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            if (!isLast) {
                TextLink(
                    text = stringResource(R.string.tutorial_skip),
                    onClick = onFinish,
                    style = SlText.LinkSm,
                    color = SlColor.TextSecondary,
                    underline = false,
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) { index ->
            TutorialPageContent(page = pages[index])
        }

        PageDots(
            count = pages.size,
            current = pagerState.currentPage,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 24.dp),
        )

        PrimaryButton(
            text = stringResource(
                when {
                    !isLast -> R.string.tutorial_next
                    replay -> R.string.tutorial_done
                    else -> R.string.tutorial_start
                },
            ),
            onClick = {
                if (isLast) {
                    onFinish()
                } else {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            },
            modifier = Modifier.padding(
                start = SlDimen.ScreenPaddingWide,
                end = SlDimen.ScreenPaddingWide,
                bottom = 24.dp,
            ),
        )
    }
}

@Composable
private fun TutorialPageContent(page: TutorialPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = SlDimen.ScreenPaddingWide),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        StepiBubble(
            modifier = Modifier.widthIn(max = 340.dp).fillMaxWidth(),
        ) {
            Text(
                text = stringResource(page.titleRes),
                style = SlText.StatusTitle,
                color = SlColor.TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(page.bodyRes, TemporaryAllow.MINUTES, TemporaryAllow.DAILY_LIMIT),
                style = SlText.Body,
                color = SlColor.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            if (page.showConditions) {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                ) {
                    ConditionChip(SlIcons.Steps, stringResource(R.string.condition_steps))
                    ConditionChip(SlIcons.Moon, stringResource(R.string.condition_sleep))
                    ConditionChip(SlIcons.Timer, stringResource(R.string.condition_pomodoro))
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        StepLockMascot(
            modifier = Modifier.size(width = 120.dp, height = 149.dp),
            mood = page.mood,
        )
    }
}

@Composable
private fun ConditionChip(icon: ImageVector, label: String) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(SlColor.BrandTintAlt)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SlColor.BrandDeep,
            modifier = Modifier.size(14.dp),
        )
        Text(text = label, style = SlText.Chip, color = SlColor.BrandDeep)
    }
}

/** 지금 몇 번째 장인지. 지금 장만 길게 늘여 색보다 모양으로 구분합니다. */
@Composable
private fun PageDots(count: Int, current: Int, modifier: Modifier = Modifier) {
    val position = stringResource(R.string.tutorial_position, current + 1, count)
    Row(
        modifier = modifier.semantics { contentDescription = position },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(count) { index ->
            Box(
                modifier = Modifier
                    .size(width = if (index == current) 20.dp else 8.dp, height = 8.dp)
                    .clip(CircleShape)
                    .background(if (index == current) SlColor.Brand else SlColor.Border),
            )
        }
    }
}

@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun TutorialScreenPreview() {
    StepLockTheme {
        TutorialScreen(onFinish = {})
    }
}

/** 다크 모드 — 같은 데이터로 색만 바뀌는지 봅니다. */
@Preview(widthDp = 412, heightDp = 892, name = "Dark")
@Composable
private fun TutorialScreenPreviewDark() {
    StepLockTheme(darkTheme = true) {
        TutorialScreen(onFinish = {})
    }
}
