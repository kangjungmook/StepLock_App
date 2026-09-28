package com.steplock.app.ui.theme

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.steplock.app.data.ThemeMode

/**
 * 앱 안에서 고른 화면 모드를 적용합니다.
 *
 * 안드로이드 12 이상에서는 **시스템에 앱 전용 모드로 등록**합니다. 그래야 Compose 가
 * 그리기 전에 뜨는 스플래시와 창 배경(values-night)까지 같은 모드를 따릅니다 —
 * Compose 쪽만 바꾸면 "다크"를 골라도 켤 때마다 밝은 스플래시가 번쩍입니다.
 * 시스템이 값을 기억하므로 앱을 다시 켤 때마다 부를 필요는 없고, 사용자가 바꿀 때만 부릅니다.
 *
 * 11 이하에는 그 기능이 없어서 Compose 화면만 바뀝니다([isDark]).
 */
object ThemeModeApplier {

    fun apply(context: Context, mode: ThemeMode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val manager = context.getSystemService(UiModeManager::class.java) ?: return
        manager.setApplicationNightMode(
            when (mode) {
                ThemeMode.System -> UiModeManager.MODE_NIGHT_AUTO
                ThemeMode.Light -> UiModeManager.MODE_NIGHT_NO
                ThemeMode.Dark -> UiModeManager.MODE_NIGHT_YES
            },
        )
    }
}

/** 지금 어두운 색을 써야 하는지. "기기 설정 따르기"면 시스템 값을 봅니다. */
@Composable
@ReadOnlyComposable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.System -> isSystemInDarkTheme()
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
}
