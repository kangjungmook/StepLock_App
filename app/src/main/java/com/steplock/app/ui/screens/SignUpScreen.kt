package com.steplock.app.ui.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.steplock.app.R
import com.steplock.app.ui.components.CheckboxMark
import com.steplock.app.ui.components.IconTapTarget
import com.steplock.app.ui.components.MascotMood
import com.steplock.app.ui.components.PrimaryButton
import com.steplock.app.ui.components.SlIcons
import com.steplock.app.ui.components.SocialLoginButton
import com.steplock.app.ui.components.SocialProvider
import com.steplock.app.ui.components.StepLockMascot
import com.steplock.app.ui.components.StepiSays
import com.steplock.app.ui.components.TextLink
import com.steplock.app.ui.components.UnderlineTextField
import com.steplock.app.ui.theme.SlColor
import com.steplock.app.ui.theme.SlDimen
import com.steplock.app.ui.theme.SlText
import com.steplock.app.ui.theme.StepLockTheme

/** 개인정보 처리방침. 저장소의 문서를 그대로 보여 줍니다. */
private const val PRIVACY_POLICY_URL =
    "https://github.com/kangjungmook/StepLock_App/blob/main/docs/privacy-policy.md"

/** 비밀번호 최소 길이. Supabase 기본값과 같습니다. */
private const val MIN_PASSWORD = 6

/**
 * 이메일 회원가입.
 *
 * 전에는 로그인 화면의 "회원가입" 글자를 누르면 **그 자리에 적힌 이메일·비밀번호로
 * 곧바로 가입 요청**이 나갔습니다. 비밀번호 확인도, 무슨 일이 일어났는지 알려 주는
 * 화면도 없어서 가입이 된 건지 알 수 없었습니다.
 *
 * 인증 메일을 보냈으면([confirmSentTo]) 입력 폼 대신 "메일함을 확인해 주세요"를
 * 보여 줍니다. 메일의 링크를 누르면 딥링크로 앱에 돌아와 바로 로그인됩니다.
 */
@Composable
fun SignUpScreen(
    onSignUp: (email: String, password: String) -> Unit,
    onSocialLogin: (SocialProvider) -> Unit,
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier,
    submitting: Boolean = false,
    errorText: String? = null,
    /** 인증 메일을 보낸 주소. null 이 아니면 입력 폼 대신 안내를 보여 줍니다. */
    confirmSentTo: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SlColor.Background)
            .safeDrawingPadding()
            .imePadding(),
    ) {
        IconTapTarget(
            icon = SlIcons.ArrowLeft,
            contentDescription = stringResource(R.string.action_back),
            onClick = onBackToLogin,
            modifier = Modifier.padding(start = 12.dp, top = 4.dp),
        )
        if (confirmSentTo != null) {
            ConfirmSent(email = confirmSentTo, onBackToLogin = onBackToLogin)
        } else {
            SignUpForm(
                onSignUp = onSignUp,
                onSocialLogin = onSocialLogin,
                onBackToLogin = onBackToLogin,
                submitting = submitting,
                errorText = errorText,
            )
        }
    }
}

@Composable
private fun SignUpForm(
    onSignUp: (email: String, password: String) -> Unit,
    onSocialLogin: (SocialProvider) -> Unit,
    onBackToLogin: () -> Unit,
    submitting: Boolean,
    errorText: String?,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var agreed by rememberSaveable { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    val passwordShort = password.isNotEmpty() && password.length < MIN_PASSWORD
    val mismatch = confirm.isNotEmpty() && confirm != password
    val ready = email.isNotBlank() &&
        password.length >= MIN_PASSWORD &&
        confirm == password &&
        agreed &&
        !submitting

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SlDimen.ScreenPadding)
            .padding(top = 8.dp, bottom = 24.dp),
    ) {
        Text(
            text = stringResource(R.string.signup_title),
            style = SlText.HomeBig.copy(fontSize = 32.sp, lineHeight = 40.sp),
            color = SlColor.TextPrimary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.signup_note),
            style = SlText.LoginNote,
            color = SlColor.TextSecondary,
        )

        Spacer(Modifier.height(32.dp))
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
            placeholder = stringResource(R.string.signup_password_placeholder, MIN_PASSWORD),
            leadingIcon = SlIcons.PasswordLock,
            isPassword = true,
            keyboardType = KeyboardType.Password,
        )
        // 규칙은 틀렸을 때만 빨갛게 — 입력하기도 전에 오류처럼 보이지 않게 합니다.
        FieldHint(
            text = stringResource(R.string.signup_password_rule, MIN_PASSWORD),
            isError = passwordShort,
        )

        Spacer(Modifier.height(16.dp))
        UnderlineTextField(
            value = confirm,
            onValueChange = { confirm = it },
            label = stringResource(R.string.signup_confirm_label),
            placeholder = stringResource(R.string.signup_confirm_placeholder),
            leadingIcon = SlIcons.PasswordLock,
            isPassword = true,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
        )
        if (mismatch) {
            FieldHint(text = stringResource(R.string.signup_confirm_mismatch), isError = true)
        }

        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(SlDimen.RadiusSmall))
                    .toggleable(
                        value = agreed,
                        role = Role.Checkbox,
                        onValueChange = { agreed = it },
                    )
                    .defaultMinSize(minHeight = SlDimen.TouchTarget)
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CheckboxMark(
                    checked = agreed,
                    size = 20.dp,
                    cornerRadius = 6.dp,
                    borderWidth = 1.5.dp,
                    checkIcon = SlIcons.CheckExtraBold,
                    checkSize = 12.dp,
                )
                Text(
                    text = stringResource(R.string.signup_agree),
                    style = SlText.Label,
                    color = SlColor.TextPrimary,
                )
            }
            TextLink(
                text = stringResource(R.string.signup_agree_view),
                onClick = { uriHandler.openUri(PRIVACY_POLICY_URL) },
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
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Spacer(Modifier.height(24.dp))
        PrimaryButton(
            text = stringResource(
                if (submitting) R.string.signup_submitting else R.string.signup_submit,
            ),
            onClick = { onSignUp(email, password) },
            enabled = ready,
        )

        // 소셜 계정은 처음 로그인할 때 계정이 만들어집니다 — 따로 가입할 필요가 없다는
        // 걸 여기서 알려 줘야, 가입 화면에 와서 이메일 폼만 보고 돌아가지 않습니다.
        Spacer(Modifier.height(32.dp))
        Text(
            text = stringResource(R.string.signup_social_divider),
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

        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.signup_login_prompt),
                style = SlText.Signup,
                color = SlColor.TextSecondary,
            )
            TextLink(
                text = stringResource(R.string.signup_login_action),
                onClick = onBackToLogin,
                style = SlText.Signup.copy(fontWeight = FontWeight.Bold),
                color = SlColor.BrandInk,
                underline = true,
            )
        }
    }
}

/** 입력 칸 아래 한 줄. 틀렸을 때만 오류 색입니다. */
@Composable
private fun FieldHint(text: String, isError: Boolean) {
    Text(
        text = text,
        style = SlText.Caption,
        color = if (isError) SlColor.Error else SlColor.TextTertiary,
        modifier = Modifier.padding(top = 8.dp),
    )
}

/** 인증 메일을 보낸 뒤. 스텝이가 다음에 할 일을 알려 줍니다. */
@Composable
private fun ConfirmSent(email: String, onBackToLogin: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = SlDimen.ScreenPaddingWide, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                StepiSays(text = stringResource(R.string.signup_sent_stepi))
                Spacer(Modifier.height(16.dp))
                StepLockMascot(
                    modifier = Modifier.size(width = 64.dp, height = 79.dp),
                    mood = MascotMood.Resting,
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.signup_sent_title),
                    style = SlText.LoginHeading,
                    color = SlColor.TextPrimary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.signup_sent_desc, email),
                    style = SlText.LoginNote,
                    color = SlColor.TextSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }
        PrimaryButton(
            text = stringResource(R.string.signup_sent_back),
            onClick = onBackToLogin,
        )
    }
}

@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun SignUpScreenPreview() {
    StepLockTheme {
        SignUpScreen(onSignUp = { _, _ -> }, onSocialLogin = {}, onBackToLogin = {})
    }
}

@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun SignUpScreenSentPreview() {
    StepLockTheme {
        SignUpScreen(
            onSignUp = { _, _ -> },
            onSocialLogin = {},
            onBackToLogin = {},
            confirmSentTo = "jiwoo@example.com",
        )
    }
}
