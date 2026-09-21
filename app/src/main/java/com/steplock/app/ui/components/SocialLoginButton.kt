package com.steplock.app.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
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

/** 아이콘만 각 브랜드 색을 유지합니다(식별 목적). 버튼 면은 팔레트를 따릅니다. */
enum class SocialProvider(@StringRes val labelRes: Int) {
    Google(R.string.login_social_google),
    Kakao(R.string.login_social_kakao),
    Apple(R.string.login_social_apple),
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
                tint = SlColor.Social.AppleInk,
                modifier = Modifier.size(19.dp),
            )
        }
    }
}
