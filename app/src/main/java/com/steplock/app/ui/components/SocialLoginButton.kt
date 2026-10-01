package com.steplock.app.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.steplock.app.R
import com.steplock.app.ui.theme.SlColor
import com.steplock.app.ui.theme.SlDimen
import com.steplock.app.ui.theme.SlText

/** 아이콘만 각 브랜드 색을 유지합니다(식별 목적). 버튼 면은 팔레트를 따릅니다. */
enum class SocialProvider(@StringRes val labelRes: Int, @StringRes val continueRes: Int) {
    Google(R.string.login_social_google, R.string.login_continue_google),
    Kakao(R.string.login_social_kakao, R.string.login_continue_kakao),
    Apple(R.string.login_social_apple, R.string.login_continue_apple),
}

@Composable
fun SocialLoginButton(
    provider: SocialProvider,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(provider.labelRes)
    val container = when (provider) {
        SocialProvider.Google -> SlColor.Background
        SocialProvider.Kakao -> SlColor.Social.Kakao
        SocialProvider.Apple -> SlColor.TextPrimary
    }
    Box(
        modifier = modifier
            .size(SlDimen.TouchTarget)
            .clip(CircleShape)
            .background(container)
            .then(
                if (provider == SocialProvider.Google) {
                    Modifier.border(1.dp, SlColor.Border, CircleShape)
                } else {
                    Modifier
                },
            )
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        when (provider) {
            SocialProvider.Google -> Image(
                imageVector = SlIcons.GoogleMark,
                contentDescription = label,
                modifier = Modifier.size(19.dp),
            )

            SocialProvider.Kakao -> Icon(
                imageVector = SlIcons.KakaoTalk,
                contentDescription = label,
                tint = SlColor.Social.KakaoInk,
                modifier = Modifier.size(20.dp),
            )

            SocialProvider.Apple -> Icon(
                imageVector = SlIcons.Apple,
                contentDescription = label,
                // 배경(TextPrimary)의 반대색 — 라이트는 검은 버튼에 밝은 로고, 다크는 그 반대.
                tint = SlColor.Background,
                modifier = Modifier.size(19.dp),
            )
        }
    }
}

/**
 * 가로 전체를 쓰는 소셜 로그인 버튼 — 로그인 화면의 첫 번째 길입니다.
 *
 * 동그란 로고 버튼은 무엇을 누르는지 로고로 알아봐야 하지만, 이 버튼은
 * "카카오로 계속하기"라고 적혀 있습니다. 로고는 왼쪽에 고정하고 글은 가운데에 둡니다.
 */
@Composable
fun SocialWideButton(
    provider: SocialProvider,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(provider.continueRes)
    val shape = RoundedCornerShape(SlDimen.RadiusCta)
    val (container, content) = when (provider) {
        SocialProvider.Google -> SlColor.Surface to SlColor.TextPrimary
        SocialProvider.Kakao -> SlColor.Social.Kakao to SlColor.Social.KakaoInk
        SocialProvider.Apple -> SlColor.TextPrimary to SlColor.Background
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(container)
            .then(
                if (provider == SocialProvider.Google) Modifier.border(1.dp, SlColor.BorderStrong, shape) else Modifier,
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.align(Alignment.CenterStart).size(20.dp), contentAlignment = Alignment.Center) {
            when (provider) {
                SocialProvider.Google -> Image(SlIcons.GoogleMark, null, Modifier.size(19.dp))
                SocialProvider.Kakao -> Icon(SlIcons.KakaoTalk, null, tint = content, modifier = Modifier.size(20.dp))
                SocialProvider.Apple -> Icon(SlIcons.Apple, null, tint = content, modifier = Modifier.size(19.dp))
            }
        }
        Text(text = label, style = SlText.RowTitle, color = content)
    }
}
