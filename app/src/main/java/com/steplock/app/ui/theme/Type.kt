@file:OptIn(ExperimentalTextApi::class)

package com.steplock.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.steplock.app.R

private val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val notoSansKr = GoogleFont("Noto Sans KR")

/** 다운로더블 폰트로 받아옵니다. ttf를 번들하려면 res/font에 넣고 이 정의만 교체하세요. */
val NotoSansKr = FontFamily(
    Font(googleFont = notoSansKr, fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = notoSansKr, fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = notoSansKr, fontProvider = fontProvider, weight = FontWeight.Bold),
    Font(googleFont = notoSansKr, fontProvider = fontProvider, weight = FontWeight.Black),
)

private fun slStyle(
    weight: FontWeight,
    size: Float,
    lineHeight: Float = size * 1.35f,
    letterSpacing: Float = 0f,
) = TextStyle(
    fontFamily = NotoSansKr,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None,
    ),
)

object SlText {
    val Wordmark = slStyle(FontWeight.Black, 32f, 36f, -1f)
    val AppTitle = slStyle(FontWeight.Black, 34f, 39f, -0.6f)
    val Greeting = slStyle(FontWeight.Black, 24f, 31f, -0.4f)
    val LockTitle = slStyle(FontWeight.Black, 25f, 32f, -0.4f)
    val RingValue = slStyle(FontWeight.Black, 42f, 42f, -1f)
    val LoginHeading = slStyle(FontWeight.Black, 20f, 26f)
    val DialogTitle = slStyle(FontWeight.Black, 19f, 26f)
    val StatusTitle = slStyle(FontWeight.Black, 18f, 25f)

    val ScreenTitle = slStyle(FontWeight.Bold, 18f, 24f)
    val RowTitle = slStyle(FontWeight.Bold, 15f, 21f)
    val Cta = slStyle(FontWeight.Bold, 16f, 22f)
    val StepperValue = slStyle(FontWeight.Bold, 17f, 22f)
    val BadgeInitial = slStyle(FontWeight.Bold, 16f, 20f)
    val BadgeInitialSmall = slStyle(FontWeight.Bold, 15f, 20f)
    val Chip = slStyle(FontWeight.Bold, 12f, 16f)
    val Remaining = slStyle(FontWeight.Bold, 14f, 20f)
    val RingPercent = slStyle(FontWeight.Bold, 11f, 14f)
    val SectionLabel = slStyle(FontWeight.Bold, 12f, 16f, 0.6f)
    val NavLabelActive = slStyle(FontWeight.Bold, 11f, 14f)

    val Tagline = slStyle(FontWeight.Normal, 15f, 24f)
    val Body = slStyle(FontWeight.Normal, 14f, 22f)
    val BodySm = slStyle(FontWeight.Normal, 13f, 20f)
    val RowValue = slStyle(FontWeight.Normal, 13f, 18f)
    val LoginNote = slStyle(FontWeight.Normal, 13.5f, 22f)
    val Input = slStyle(FontWeight.Normal, 14f, 20f)
    val AppSubtitle = slStyle(FontWeight.Normal, 12f, 16f)
    val Caption = slStyle(FontWeight.Normal, 12f, 19f)
    val Signup = slStyle(FontWeight.Normal, 13f, 20f)

    val ListItem = slStyle(FontWeight.Medium, 15f, 20f)
    val StepperLabel = slStyle(FontWeight.Medium, 14f, 20f)
    val CaptionMedium = slStyle(FontWeight.Medium, 13f, 21f)
    val Label = slStyle(FontWeight.Medium, 12.5f, 18f)
    val LabelSm = slStyle(FontWeight.Medium, 12f, 16f)
    val Link = slStyle(FontWeight.Medium, 13.5f, 20f)
    val LinkSm = slStyle(FontWeight.Medium, 13f, 20f)
    val NavLabel = slStyle(FontWeight.Medium, 11f, 14f)
}

val SlTypography = Typography(
    headlineLarge = SlText.AppTitle,
    headlineMedium = SlText.Greeting,
    titleLarge = SlText.ScreenTitle,
    titleMedium = SlText.RowTitle,
    bodyLarge = SlText.Tagline,
    bodyMedium = SlText.Body,
    bodySmall = SlText.BodySm,
    labelLarge = SlText.Cta,
    labelMedium = SlText.Label,
    labelSmall = SlText.Caption,
)
