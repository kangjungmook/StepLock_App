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
import com.steplock.app.ads.Ads
import com.steplock.app.ads.rememberRewardedAd
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

        // 이미 받아 둔 동의만 확인합니다. 여기서 동의 폼을 띄우면, 차단된 앱을
        // 열었을 뿐인데 처음 보는 화면이 덮여 무슨 일인지 알 수 없습니다.
        Ads.refreshWithoutForm(this)

        setContent {
            val context = LocalContext.current
            val stepLockViewModel: StepLockViewModel =
                viewModel(factory = StepLockViewModel.factory(context))
            val state by stepLockViewModel.uiState.collectAsStateWithLifecycle()

            // 화면이 열릴 때 미리 한 편 받아 둡니다. 누른 뒤에 불러오면
            // 몇 초간 아무 반응이 없어 고장처럼 보입니다.
            val rewardedAd = rememberRewardedAd()

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
                    adBonusRemaining = loaded.temporaryAllowBonusRemaining,
                    // 받아 둔 광고가 없으면 선택지를 아예 주지 않습니다.
                    onWatchAdForBonus = if (rewardedAd.ready) {
                        {
                            rewardedAd.show(this@LockActivity) {
                                // 끝까지 본 경우에만 불립니다. 횟수만 늘어나고
                                // 잠금은 그대로 — 쓸지는 한 번 더 눌러 정합니다.
                                stepLockViewModel.grantTemporaryAllowBonus()
                            }
                        }
                    } else {
                        null
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
