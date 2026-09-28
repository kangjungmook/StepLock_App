package com.steplock.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.steplock.app.ads.Ads
import com.steplock.app.data.SettingsRepository
import com.steplock.app.data.SupabaseProvider
import com.steplock.app.data.ThemeMode
import com.steplock.app.navigation.StepLockNavHost
import com.steplock.app.service.AppWatchService
import com.steplock.app.system.PermissionStep
import com.steplock.app.system.nextPermissionStep
import com.steplock.app.ui.theme.StepLockTheme
import com.steplock.app.ui.theme.isDark
import io.github.jan.supabase.auth.handleDeeplinks

class MainActivity : ComponentActivity() {

    /**
     * 잠금 화면에서 "집중 타이머" 로 들어온 요청. 내비게이션이 한 번 처리하면
     * 비웁니다 — 남겨 두면 화면을 돌릴 때마다 타이머로 다시 끌려갑니다.
     */
    private val openFocus = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        // super.onCreate 보다 먼저 불러야 스플래시가 화면을 이어받습니다.
        // 뒤로 밀면 흰 화면이 한 번 스쳤다가 스플래시가 뜹니다.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        SupabaseProvider.client.handleDeeplinks(intent)
        if (savedInstanceState == null) openFocus.value = intent.wantsFocus()
        // 동의 확인 → SDK 초기화. 잠금 화면에서 리워드 광고를 쓸 수 있으려면
        // 그보다 먼저 여기서 끝나 있어야 합니다.
        Ads.prepare(this)
        val themeModes = SettingsRepository(this).themeMode
        setContent {
            // 처음 한 프레임은 기기 설정으로 그립니다. 저장값은 금방 올라오고,
            // 안드로이드 12+ 는 시스템이 이미 같은 모드로 띄워 두어 차이가 없습니다.
            val themeMode by themeModes.collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val dark = themeMode.isDark()

            // 상태 표시줄 아이콘 색도 앱의 모드를 따라야 합니다. 기본값은 기기 설정을
            // 보므로, 앱에서 다크를 골랐는데 기기가 라이트면 어두운 바탕에 어두운 아이콘이 됩니다.
            DisposableEffect(dark) {
                val style = SystemBarStyle.auto(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT,
                ) { dark }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }

            StepLockTheme(darkTheme = dark) {
                StepLockNavHost(
                    openFocus = openFocus.value,
                    onFocusOpened = { openFocus.value = false },
                )
            }
        }
    }

    /** 소셜 로그인은 브라우저를 거쳐 딥링크로 돌아옵니다. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        SupabaseProvider.client.handleDeeplinks(intent)
        if (intent.wantsFocus()) openFocus.value = true
    }

    private fun Intent.wantsFocus(): Boolean = getBooleanExtra(EXTRA_OPEN_FOCUS, false)

    companion object {
        private const val EXTRA_OPEN_FOCUS = "open_focus"

        fun openFocusIntent(context: Context): Intent =
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_OPEN_FOCUS, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }

    /** 권한이 갖춰져 있으면 앱을 열 때마다 감시 서비스를 다시 살려 둡니다. */
    override fun onStart() {
        super.onStart()
        if (nextPermissionStep(this) == PermissionStep.Ready) {
            AppWatchService.start(this)
        }
    }
}
