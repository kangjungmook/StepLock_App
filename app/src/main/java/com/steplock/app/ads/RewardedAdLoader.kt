package com.steplock.app.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.steplock.app.BuildConfig

/**
 * 리워드 광고 한 편을 미리 받아 들고 있습니다.
 *
 * 누른 뒤에 불러오면 몇 초를 기다려야 하므로 화면이 열릴 때 미리 받아 둡니다.
 * 받아 두지 못했으면 [ready] 가 false 로 남고, 화면은 버튼을 아예 숨깁니다 —
 * 눌렀는데 아무 일도 일어나지 않는 것보다 없는 편이 낫습니다.
 */
class RewardedAdLoader internal constructor(
    private val context: Context,
    private val unitId: String,
) {

    var ready by mutableStateOf(false)
        private set

    private var loading = false
    private var ad: RewardedAd? = null

    fun load() {
        if (loading || ad != null || !Ads.canRequestAds) return
        loading = true
        RewardedAd.load(
            context,
            unitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(loaded: RewardedAd) {
                    loading = false
                    ad = loaded
                    ready = true
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    // 네트워크가 없거나 채울 광고가 없을 때도 여기로 옵니다.
                    // 실패를 사용자에게 알리지 않습니다 — 애초에 없던 선택지가 됩니다.
                    Log.w(TAG, "리워드 광고를 받지 못했습니다: ${error.message}")
                    loading = false
                    ad = null
                    ready = false
                }
            },
        )
    }

    /**
     * 광고를 띄웁니다. [onEarned] 는 **끝까지 본 경우에만** 불립니다 —
     * 중간에 닫으면 보상이 없어야 합니다.
     *
     * 광고가 닫힌 뒤 다음 편을 미리 받아 두지 않습니다. 하루 보너스가 두 번뿐이라
     * 대부분 다시 쓰이지 않고, 쓰지 않을 광고를 받아 두면 노출률만 나빠집니다.
     */
    fun show(activity: Activity, onEarned: () -> Unit) {
        val current = ad ?: return
        // 보여 주는 순간 손에서 놓습니다. 같은 광고를 두 번 띄우면 SDK가 거부합니다.
        ad = null
        ready = false

        current.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w(TAG, "리워드 광고를 띄우지 못했습니다: ${error.message}")
            }
        }
        current.show(activity) { onEarned() }
    }

    private companion object {
        const val TAG = "StepLockAds"
    }
}

/**
 * 화면에서 쓰는 형태. 동의가 확인되면 곧바로 한 편 받아 둡니다.
 */
@Composable
fun rememberRewardedAd(unitId: String = BuildConfig.ADMOB_REWARDED_UNIT): RewardedAdLoader {
    val context = LocalContext.current
    val loader = remember(unitId) { RewardedAdLoader(context.applicationContext, unitId) }
    val canRequestAds = Ads.canRequestAds
    LaunchedEffect(canRequestAds) {
        if (canRequestAds) loader.load()
    }
    return loader
}
