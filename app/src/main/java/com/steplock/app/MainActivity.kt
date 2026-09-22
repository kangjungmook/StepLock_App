package com.steplock.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.steplock.app.ads.Ads
import com.steplock.app.data.SupabaseProvider
import com.steplock.app.navigation.StepLockNavHost
import com.steplock.app.service.AppWatchService
import com.steplock.app.system.PermissionStep
import com.steplock.app.system.nextPermissionStep
import com.steplock.app.ui.theme.StepLockTheme
import io.github.jan.supabase.auth.handleDeeplinks

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // super.onCreate 보다 먼저 불러야 스플래시가 화면을 이어받습니다.
        // 뒤로 밀면 흰 화면이 한 번 스쳤다가 스플래시가 뜹니다.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        SupabaseProvider.client.handleDeeplinks(intent)
        // 동의 확인 → SDK 초기화. 잠금 화면에서 리워드 광고를 쓸 수 있으려면
        // 그보다 먼저 여기서 끝나 있어야 합니다.
        Ads.prepare(this)
        setContent {
            StepLockTheme {
                StepLockNavHost()
            }
        }
    }

    /** 소셜 로그인은 브라우저를 거쳐 딥링크로 돌아옵니다. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        SupabaseProvider.client.handleDeeplinks(intent)
    }

    /** 권한이 갖춰져 있으면 앱을 열 때마다 감시 서비스를 다시 살려 둡니다. */
    override fun onStart() {
        super.onStart()
        if (nextPermissionStep(this) == PermissionStep.Ready) {
            AppWatchService.start(this)
        }
    }
}
