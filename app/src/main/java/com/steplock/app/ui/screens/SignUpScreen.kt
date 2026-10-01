package com.steplock.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
 * 이메일 회원가입 — **한 화면에 한 질문씩, 세 단계.**
 *
 * 입력 칸 셋 + 동의 + 소셜 버튼이 한 화면에 쌓여 있으면 어디부터 채울지, 무엇이
 * 틀렸는지가 한눈에 안 들어옵니다. 이제 "어떤 이메일로 → 비밀번호 → 동의" 순서로
 * 한 번에 하나만 묻고, 그 단계가 맞아야 "다음"이 눌립니다. 위의 표식 세 칸이
 * 몇 단계 남았는지 보여 주고, 뒤로 가기는 이전 단계로 갑니다.
 *
 * 인증 메일을 보냈으면([confirmSentTo]) 입력 대신 "메일함을 확인해 주세요"를
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
        if (confirmSentTo != null) {
            ConfirmSent(email = confirmSentTo, onBackToLogin = onBackToLogin)
        } else {
            SignUpSteps(
                onSignUp = onSignUp,
                onSocialLogin = onSocialLogin,
                onBackToLogin = onBackToLogin,
                submitting = submitting,
                errorText = errorText,
            )
        }
    }
}

private const val STEP_EMAIL = 0
private const val STEP_PASSWORD = 1
private const val STEP_AGREE = 2
private const val STEP_COUNT = 3

private val EMAIL_SHAPE = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

@Composable
private fun ColumnScope.SignUpSteps(
    onSignUp: (email: String, password: String) -> Unit,
    onSocialLogin: (SocialProvider) -> Unit,
    onBackToLogin: () -> Unit,
    submitting: Boolean,
    errorText: String?,
) {
    var step by rememberSaveable { mutableIntStateOf(STEP_EMAIL) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var agreed by rememberSaveable { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    val emailOk = EMAIL_SHAPE.matches(email.trim())
    val passwordShort = password.isNotEmpty() && password.length < MIN_PASSWORD
    val mismatch = confirm.isNotEmpty() && confirm != password
    val passwordOk = password.length >= MIN_PASSWORD && confirm == password
    val canGoOn = when (step) {
        STEP_EMAIL -> emailOk
        STEP_PASSWORD -> passwordOk
        else -> emailOk && passwordOk && agreed && !submitting
    }
    val goBack: () -> Unit = { if (step > STEP_EMAIL) step-- else onBackToLogin() }
    BackHandler(onBack = goBack)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = SlDimen.ScreenPadding, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTapTarget(
            icon = SlIcons.ArrowLeft,
            contentDescription = stringResource(R.string.action_back),
            onClick = goBack,
        )
        Spacer(Modifier.weight(1f))
        StepMarks(current = step)
    }

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SlDimen.ScreenPadding)
            .padding(top = 16.dp, bottom = 24.dp),
    ) {
        Text(
            text = stringResource(
                when (step) {
                    STEP_EMAIL -> R.string.signup_q_email
                    STEP_PASSWORD -> R.string.signup_q_password
                    else -> R.string.signup_q_agree
                },
            ),
            style = SlText.HomeBig.copy(fontSize = 32.sp, lineHeight = 40.sp),
            color = SlColor.TextPrimary,
        )
        if (step == STEP_EMAIL) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.signup_note),
                style = SlText.Tagline,
                color = SlColor.TextSecondary,
            )
        }
        Spacer(Modifier.height(32.dp))

        when (step) {
            STEP_EMAIL -> {
                UnderlineTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = stringResource(R.string.login_email_label),
                    placeholder = stringResource(R.string.login_email_placeholder),
                    leadingIcon = SlIcons.Mail,
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done,
                )
                if (email.isNotBlank() && !emailOk) {
                    FieldHint(text = stringResource(R.string.signup_email_invalid), isError = true)
                }
                // 소셜 계정은 처음 로그인할 때 계정이 만들어집니다 — 가입 화면에 와서
                // 이메일 칸만 보고 돌아가지 않도록 여기서 알려 줍니다.
                Spacer(Modifier.height(40.dp))
                Text(
                    text = stringResource(R.string.signup_social_divider),
                    style = SlText.Label,
                    color = SlColor.TextSecondary,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SocialProvider.entries.forEach { provider ->
                        SocialLoginButton(provider = provider, onClick = { onSocialLogin(provider) })
                    }
                }
            }

            STEP_PASSWORD -> {
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
                Spacer(Modifier.height(24.dp))
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
            }

            else -> {
                // 무엇으로 가입하는지 마지막으로 한 번 보여 줍니다.
                Text(
                    text = stringResource(R.string.signup_email_summary),
                    style = SlText.LabelSm,
                    color = SlColor.TextSecondary,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = email.trim(),
                        style = SlText.RowTitle.copy(fontSize = 17.sp, lineHeight = 24.sp),
                        color = SlColor.TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    TextLink(
                        text = stringResource(R.string.signup_change_email),
                        onClick = { step = STEP_EMAIL },
                        style = SlText.Label,
                        color = SlColor.BrandInk,
                        underline = false,
                    )
                }
                Spacer(Modifier.height(24.dp))
                AgreeRow(
                    agreed = agreed,
                    onAgreedChange = { agreed = it },
                    onView = { uriHandler.openUri(PRIVACY_POLICY_URL) },
                )
                if (errorText != null) {
                    Text(
                        text = errorText,
                        style = SlText.Label,
                        color = SlColor.Error,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        }
    }

    Column(
        modifier = Modifier.padding(
            start = SlDimen.ScreenPadding,
            end = SlDimen.ScreenPadding,
            bottom = 16.dp,
        ),
    ) {
        PrimaryButton(
            text = stringResource(
                when {
                    step < STEP_AGREE -> R.string.signup_next
                    submitting -> R.string.signup_submitting
                    else -> R.string.signup_submit
                },
            ),
            onClick = {
                if (step < STEP_AGREE) step++ else onSignUp(email.trim(), password)
            },
            enabled = canGoOn,
        )
        if (step == STEP_EMAIL) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
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
}

/** 몇 번째 단계인지 — 등산로 표식과 같은 칸 세 개와 "1 / 3". */
@Composable
private fun StepMarks(current: Int) {
    val description = stringResource(R.string.signup_step, current + 1, STEP_COUNT)
    Row(
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(STEP_COUNT) { index ->
            Box(
                modifier = Modifier
                    .size(width = 20.dp, height = 8.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        when {
                            index == current -> SlColor.Brand
                            index < current -> SlColor.TextPrimary
                            else -> SlColor.TrackOff
                        },
                    ),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "${current + 1} / $STEP_COUNT",
            style = SlText.TrailTick.copy(fontSize = 15.sp, color = SlColor.TextSecondary),
        )
    }
}

/** 동의 한 줄 — 줄 전체가 체크 영역이고, 오른쪽 "보기"만 따로 눌립니다. */
@Composable
private fun AgreeRow(agreed: Boolean, onAgreedChange: (Boolean) -> Unit, onView: () -> Unit) {
    val shape = RoundedCornerShape(SlDimen.RadiusCta)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, if (agreed) SlColor.TextPrimary else SlColor.Border, shape)
            .padding(start = 4.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .toggleable(value = agreed, role = Role.Checkbox, onValueChange = onAgreedChange)
                .defaultMinSize(minHeight = 56.dp)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CheckboxMark(
                checked = agreed,
                size = 22.dp,
                cornerRadius = 6.dp,
                borderWidth = 1.5.dp,
                checkIcon = SlIcons.CheckExtraBold,
                checkSize = 13.dp,
            )
            Text(
                text = stringResource(R.string.signup_agree),
                style = SlText.RowTitle,
                color = SlColor.TextPrimary,
            )
        }
        TextLink(
            text = stringResource(R.string.signup_agree_view),
            onClick = onView,
            style = SlText.Label,
            color = SlColor.BrandInk,
            underline = false,
        )
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

/** 인증 메일을 보낸 뒤 — 큰 제목, 보낸 주소, 돌아가는 버튼 하나. */
@Composable
private fun ConfirmSent(email: String, onBackToLogin: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = SlDimen.ScreenPadding, vertical = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(40.dp))
            StepLockMascot(
                modifier = Modifier.size(width = 64.dp, height = 79.dp),
                mood = MascotMood.Resting,
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.signup_sent_title),
                style = SlText.HomeBig.copy(fontSize = 32.sp, lineHeight = 40.sp),
                color = SlColor.TextPrimary,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = email,
                style = SlText.RowTitle.copy(fontSize = 17.sp, lineHeight = 24.sp),
                color = SlColor.TextPrimary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.signup_sent_desc_short),
                style = SlText.Tagline,
                color = SlColor.TextSecondary,
            )
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
