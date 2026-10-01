package com.steplock.app.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.steplock.app.R
import com.steplock.app.ui.LoginError
import com.steplock.app.ui.LoginNotice
import com.steplock.app.ui.components.PrimaryButton
import com.steplock.app.ui.components.SlIcons
import com.steplock.app.ui.components.SocialProvider
import com.steplock.app.ui.components.SocialWideButton
import com.steplock.app.ui.components.TrailMap
import com.steplock.app.ui.components.TextLink
import com.steplock.app.ui.components.UnderlineTextField
import com.steplock.app.ui.components.Wordmark
import com.steplock.app.ui.theme.SlColor
import com.steplock.app.ui.theme.SlDimen
import com.steplock.app.ui.theme.SlText
import com.steplock.app.ui.theme.StepLockTheme

/** 같은 화면을 앱 시작·구독·동기화·친구초대 시점에 재사용하고, 안내 문구만 바꿉니다. */
enum class LoginTrigger(@StringRes val noteRes: Int) {
    AppStart(R.string.login_note_app_start),
    Subscription(R.string.login_note_subscription),
    Sync(R.string.login_note_sync),
    Social(R.string.login_note_social),
}

@StringRes
internal fun LoginError.messageRes(): Int = when (this) {
    LoginError.InvalidEmail -> R.string.login_error_email
    LoginError.ShortPassword -> R.string.login_error_password
    LoginError.SignInFailed -> R.string.login_error_sign_in
    LoginError.SignUpFailed -> R.string.login_error_sign_up
    LoginError.SocialFailed -> R.string.login_error_social
    LoginError.ResetFailed -> R.string.login_error_reset
}

@StringRes
internal fun LoginNotice.messageRes(): Int = when (this) {
    LoginNotice.PasswordResetSent -> R.string.login_reset_sent
}

@Composable
fun LoginScreen(
    onLogin: (email: String, password: String) -> Unit,
    /** 회원가입 화면으로 갑니다. 여기서 바로 가입 요청을 보내지 않습니다. */
    onSignUp: () -> Unit,
    onSocialLogin: (SocialProvider) -> Unit,
    onGuestContinue: () -> Unit,
    onForgotPassword: (email: String) -> Unit,
    modifier: Modifier = Modifier,
    trigger: LoginTrigger = LoginTrigger.AppStart,
    submitting: Boolean = false,
    errorText: String? = null,
    noticeText: String? = null,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    // 이메일 칸은 처음엔 접어 둡니다 — 대부분은 소셜 버튼 한 번으로 끝나서,
    // 칸 두 개와 링크 세 개가 첫눈에 보일 이유가 없습니다.
    var emailOpen by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SlColor.Background)
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SlDimen.ScreenPadding),
    ) {
        // 1. 등산로 입구 — 스텝이가 길 맨 아래에 서 있고, 왼쪽에 한 문장.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(HERO_HEIGHT),
        ) {
            TrailMap(
                progress = 0.04f,
                modifier = Modifier
                    .fillMaxSize()
                    .clearAndSetSemantics {},
            )
            Column(modifier = Modifier.fillMaxWidth(0.62f).padding(top = 24.dp)) {
                Wordmark(text = stringResource(R.string.app_wordmark))
                Spacer(Modifier.height(32.dp))
                Text(
                    text = stringResource(R.string.login_title),
                    style = SlText.HomeBig,
                    color = SlColor.TextPrimary,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(trigger.noteRes),
                    style = SlText.Tagline,
                    color = SlColor.TextSecondary,
                )
            }
        }

        if (errorText != null || noticeText != null) {
            Text(
                text = errorText ?: noticeText.orEmpty(),
                style = SlText.Label,
                color = if (errorText != null) SlColor.Error else SlColor.BrandInk,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }

        // 2. 가장 빠른 길 — 소셜 계정.
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SocialProvider.entries.forEach { provider ->
                SocialWideButton(provider = provider, onClick = { onSocialLogin(provider) })
            }
        }

        // 3. 이메일 — 누르면 그 자리에서 펼쳐집니다.
        Spacer(Modifier.height(24.dp))
        if (!emailOpen) {
            PrimaryButton(
                text = stringResource(R.string.login_email_open),
                onClick = { emailOpen = true },
                height = 52.dp,
                containerColor = SlColor.SurfaceAlt,
                contentColor = SlColor.TextPrimary,
            )
        } else {
            Text(
                text = stringResource(R.string.login_email_section),
                style = SlText.SectionTitle,
                color = SlColor.TextPrimary,
            )
            Spacer(Modifier.height(16.dp))
            UnderlineTextField(
                value = email,
                onValueChange = { email = it },
                label = stringResource(R.string.login_email_label),
                placeholder = stringResource(R.string.login_email_placeholder),
                leadingIcon = SlIcons.Mail,
                keyboardType = KeyboardType.Email,
            )
            Spacer(Modifier.height(24.dp))
            UnderlineTextField(
                value = password,
                onValueChange = { password = it },
                label = stringResource(R.string.login_password_label),
                placeholder = stringResource(R.string.login_password_label),
                leadingIcon = SlIcons.PasswordLock,
                isPassword = true,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            )
            // "로그인 유지" 체크박스를 두지 않습니다. 세션은 언제나 기기에 남아서
            // 체크를 풀어도 달라지는 게 없었습니다 — 동작하지 않는 선택지였습니다.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = SlDimen.TouchTarget),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextLink(
                    text = stringResource(R.string.login_forgot),
                    onClick = { onForgotPassword(email) },
                    style = SlText.Label,
                    color = SlColor.BrandInk,
                    underline = false,
                )
            }
            Spacer(Modifier.height(8.dp))
            PrimaryButton(
                text = if (submitting) {
                    stringResource(R.string.login_submitting)
                } else {
                    stringResource(R.string.login_submit)
                },
                onClick = { onLogin(email, password) },
                enabled = !submitting,
            )
        }

        // 4. 처음이거나, 아직 계정이 필요 없거나.
        Spacer(Modifier.height(32.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.login_signup_prompt),
                style = SlText.Signup,
                color = SlColor.TextSecondary,
            )
            TextLink(
                text = stringResource(R.string.login_signup_action),
                onClick = onSignUp,
                style = SlText.Signup.copy(fontWeight = FontWeight.Bold),
                color = SlColor.BrandInk,
                underline = true,
            )
        }
        TextLink(
            text = stringResource(R.string.login_guest),
            onClick = onGuestContinue,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
    }
}

private val HERO_HEIGHT = 300.dp

@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun LoginScreenPreview() {
    StepLockTheme {
        LoginScreen(
            onLogin = { _, _ -> },
            onSignUp = {},
            onSocialLogin = {},
            onGuestContinue = {},
            onForgotPassword = {},
        )
    }
}

@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun LoginScreenSyncPreview() {
    StepLockTheme {
        LoginScreen(
            onLogin = { _, _ -> },
            onSignUp = {},
            onSocialLogin = {},
            onGuestContinue = {},
            onForgotPassword = {},
            trigger = LoginTrigger.Sync,
        )
    }
}
