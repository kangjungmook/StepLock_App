package com.steplock.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val StepLockColorScheme = lightColorScheme(
    primary = SlColor.Brand,
    onPrimary = SlColor.OnBrand,
    primaryContainer = SlColor.BrandTint,
    onPrimaryContainer = SlColor.BrandDeep,
    secondary = SlColor.Amber,
    onSecondary = SlColor.OnAmber,
    secondaryContainer = SlColor.AmberSurface,
    onSecondaryContainer = SlColor.AmberText,
    background = SlColor.Background,
    onBackground = SlColor.TextPrimary,
    surface = SlColor.Surface,
    onSurface = SlColor.TextPrimary,
    surfaceVariant = SlColor.SurfaceAlt,
    onSurfaceVariant = SlColor.TextSecondary,
    outline = SlColor.Border,
    outlineVariant = SlColor.BorderStrong,
)

/**
 * 시안의 다크 팔레트는 잠금 오버레이 전용이라 시스템 다크모드를 따르지 않습니다.
 * 오버레이는 SlColor.Dark 토큰을 직접 사용합니다.
 */
@Composable
fun StepLockTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = StepLockColorScheme,
        typography = SlTypography,
        content = content,
    )
}
