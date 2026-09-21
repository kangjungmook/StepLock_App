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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.steplock.app.R
import com.steplock.app.ui.LoginError
import com.steplock.app.ui.LoginNotice
import com.steplock.app.ui.components.CheckboxMark
import com.steplock.app.ui.components.PrimaryButton
import com.steplock.app.ui.components.SlIcons
import com.steplock.app.ui.components.SocialLoginButton
import com.steplock.app.ui.components.SocialProvider
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
    onLogin: (email: String, password: String, rememberMe: Boolean) -> Unit,
    onSignUp: (email: String, password: String) -> Unit,
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
    var rememberMe by rememberSaveable { mutableStateOf(true) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SlColor.Surface)
            .safeDrawingPadding()
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SlDimen.ScreenPaddingWide, vertical = 24.dp),
        ) {
            Wordmark(text = stringResource(R.string.app_wordmark))

            Spacer(Modifier.height(44.dp))
            Text(
                text = stringResource(R.string.login_title),
                style = SlText.LoginHeading,
                color = SlColor.Brand,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(trigger.noteRes),
                style = SlText.LoginNote,
                color = SlColor.TextSecondary,
            )

            Spacer(Modifier.height(28.dp))
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = SlDimen.TouchTarget),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(SlDimen.RadiusSmall))
                        .toggleable(
                            value = rememberMe,
                            role = Role.Checkbox,
                            onValueChange = { rememberMe = it },
                        )
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CheckboxMark(
                        checked = rememberMe,
                        size = 18.dp,
                        cornerRadius = 5.dp,
                        borderWidth = 1.5.dp,
                        checkIcon = SlIcons.CheckExtraBold,
                        checkSize = 11.dp,
                    )
                    Text(
                        text = stringResource(R.string.login_remember),
                        style = SlText.Label,
                        color = SlColor.TextSecondary,
                    )
                }
                TextLink(
                    text = stringResource(R.string.login_forgot),
                    onClick = { onForgotPassword(email) },
                    style = SlText.Label,
                    color = SlColor.BrandInk,
                    underline = false,
                )
            }

            if (errorText != null) {
                Text(
                    text = errorText,
                    style = SlText.Label,
                    color = SlColor.Error,
                    modifier = Modifier.padding(top = 4.dp),
                )
            } else if (noticeText != null) {
                Text(
                    text = noticeText,
                    style = SlText.Label,
                    color = SlColor.BrandInk,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Spacer(Modifier.height(24.dp))
            PrimaryButton(
                text = if (submitting) {
                    stringResource(R.string.login_submitting)
                } else {
                    stringResource(R.string.login_submit)
                },
                onClick = { onLogin(email, password, rememberMe) },
                shape = CircleShape,
                enabled = !submitting,
            )

            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.login_social_divider),
                style = SlText.Label,
                color = SlColor.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            ) {
                SocialProvider.entries.forEach { provider ->
                    SocialLoginButton(provider = provider, onClick = { onSocialLogin(provider) })
                }
            }

            Spacer(Modifier.height(12.dp))
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
                    onClick = { onSignUp(email, password) },
                    style = SlText.Signup.copy(fontWeight = FontWeight.Bold),
                    color = SlColor.BrandInk,
                    underline = true,
                )
            }

            Spacer(Modifier.height(4.dp))
            TextLink(
                text = stringResource(R.string.login_guest),
                onClick = onGuestContinue,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun LoginScreenPreview() {
    StepLockTheme {
        LoginScreen(
            onLogin = { _, _, _ -> },
            onSignUp = { _, _ -> },
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
            onLogin = { _, _, _ -> },
            onSignUp = { _, _ -> },
            onSocialLogin = {},
            onGuestContinue = {},
            onForgotPassword = {},
            trigger = LoginTrigger.Sync,
        )
    }
}
