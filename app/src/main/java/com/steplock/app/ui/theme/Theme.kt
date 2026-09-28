package com.steplock.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * 시스템 다크 모드를 따릅니다. 화면 코드는 [SlColor] 토큰만 쓰고, 여기서 내려 준
 * 팔레트에 따라 값이 바뀝니다.
 *
 * 잠금 화면은 이 설정과 상관없이 [SlColor.Dark] 를 씁니다.
 */
@Composable
fun StepLockTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val palette = if (darkTheme) DarkPalette else LightPalette
    CompositionLocalProvider(LocalSlPalette provides palette) {
        MaterialTheme(
            colorScheme = palette.toColorScheme(darkTheme),
            typography = SlTypography,
            content = content,
        )
    }
}

/** Material 컴포넌트(대화상자·텍스트 선택 등)도 같은 색을 쓰게 맞춥니다. */
private fun SlPalette.toColorScheme(dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = brand,
        onPrimary = onBrand,
        primaryContainer = brandTint,
        onPrimaryContainer = brandDeep,
        secondary = amber,
        onSecondary = onAmber,
        secondaryContainer = amberSurface,
        onSecondaryContainer = amberText,
        background = background,
        onBackground = textPrimary,
        surface = surface,
        onSurface = textPrimary,
        surfaceVariant = surfaceAlt,
        onSurfaceVariant = textSecondary,
        surfaceContainerHigh = surface,
        surfaceContainerHighest = surfaceAlt,
        outline = border,
        outlineVariant = borderStrong,
        error = error,
        onError = onError,
    )
}
