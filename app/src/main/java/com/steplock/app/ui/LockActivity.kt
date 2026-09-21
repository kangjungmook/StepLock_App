package com.steplock.app.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.steplock.app.data.BlockedAppCatalog
import com.steplock.app.ui.screens.LockOverlayScreen
import com.steplock.app.ui.theme.SlColor
import kotlinx.coroutines.launch

/**
 * 차단한 앱이 열릴 때 그 위에 덮이는 화면. 감시 서비스가 띄웁니다.
 * 뒤로 가기와 닫기는 홈으로 보냅니다 — 차단된 앱으로 되돌아가지 않게.
 */
class LockActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = BlockedAppCatalog.byId(intent.getStringExtra(EXTRA_APP_ID).orEmpty())
            ?: BlockedAppCatalog.apps.first()

        onBackPressedDispatcher.addCallback(this) { goHome() }

        setContent {
            val context = LocalContext.current
            val stepLockViewModel: StepLockViewModel =
                viewModel(factory = StepLockViewModel.factory(context))
            val state by stepLockViewModel.uiState.collectAsStateWithLifecycle()

            val scope = rememberCoroutineScope()
            val loaded = state
            if (loaded == null) {
                Box(Modifier.fillMaxSize().background(SlColor.Dark.Background))
            } else {
                LockOverlayScreen(
                    appName = app.name,
                    stat = loaded.today,
                    settings = loaded.settings,
                    temporaryAllowRemaining = loaded.temporaryAllowRemaining,
                    onDismiss = { goHome() },
                    onTemporaryAllow = {
                        // 저장이 끝난 뒤에 닫습니다. 먼저 닫으면 감시 서비스가
                        // 아직 옛 값을 보고 잠금을 다시 띄울 수 있습니다.
                        scope.launch {
                            if (stepLockViewModel.useTemporaryAllow()) finish()
                        }
                    },
                )
            }
        }
    }

    private fun goHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        finish()
    }

    companion object {
        private const val EXTRA_APP_ID = "app_id"

        fun intent(context: Context, appId: String): Intent =
            Intent(context, LockActivity::class.java)
                .putExtra(EXTRA_APP_ID, appId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
}
