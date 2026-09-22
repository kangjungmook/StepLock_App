package com.steplock.app.ads

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * 광고를 요청해도 되는지 판단하고, 되는 시점에 SDK를 켭니다.
 *
 * 순서가 뒤집히면 안 됩니다: **동의 확인 → 초기화 → 광고 요청**. 동의를 받기 전에
 * 광고를 요청하면 유럽 사용자에게 광고가 아예 나가지 않고 정책 위반이 됩니다.
 *
 * 동의 확인이 실패하더라도 앱 기능은 그대로 돌아가야 하므로, 실패는 로그만 남기고
 * [canRequestAds] 를 false 로 둡니다 — 광고만 빠지고 잠금·걸음·통계는 정상입니다.
 */
object Ads {

    private const val TAG = "StepLockAds"

    /**
     * 동의가 확인되고 SDK가 켜져서 광고를 요청해도 되는 상태.
     * Compose 상태라서 화면이 이 값을 그대로 관찰합니다.
     */
    var canRequestAds by mutableStateOf(false)
        private set

    /** 설정 화면에 "광고 동의 다시 설정" 줄을 보여 줄지 — 유럽 등에서만 필요합니다. */
    var privacyOptionsRequired by mutableStateOf(false)
        private set

    private var sdkStarted = false
    private var consentRequested = false

    /**
     * 앱을 열 때 한 번 호출합니다. 화면이 다시 만들어져도 동의 폼이 두 번 뜨지 않게
     * 요청 자체는 한 번만 보냅니다.
     */
    fun prepare(activity: Activity) {
        if (consentRequested) {
            syncConsentState(activity)
            return
        }
        consentRequested = true

        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()

        UserMessagingPlatform.getConsentInformation(activity).requestConsentInfoUpdate(
            activity,
            params,
            {
                // 폼이 필요 없는 지역이면 아무것도 띄우지 않고 바로 콜백이 옵니다.
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Log.w(TAG, "동의 폼을 띄우지 못했습니다: ${formError.message}")
                    }
                    syncConsentState(activity)
                }
            },
            { requestError ->
                Log.w(TAG, "동의 정보를 받지 못했습니다: ${requestError.message}")
                syncConsentState(activity)
            },
        )
    }

    /**
     * 폼을 띄우지 않고, 이미 받아 둔 동의만 다시 확인합니다.
     *
     * 잠금 화면에서 씁니다. 차단한 앱을 열었을 뿐인데 동의 폼이 덮어 뜨면
     * 사용자는 이게 무슨 화면인지 알 수 없습니다. 아직 동의를 받은 적이 없다면
     * [canRequestAds] 가 false 로 남고 광고 버튼이 그냥 보이지 않습니다.
     */
    fun refreshWithoutForm(activity: Activity) {
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()

        UserMessagingPlatform.getConsentInformation(activity).requestConsentInfoUpdate(
            activity,
            params,
            { syncConsentState(activity) },
            { requestError ->
                Log.w(TAG, "동의 정보를 받지 못했습니다: ${requestError.message}")
                syncConsentState(activity)
            },
        )
    }

    /** 설정 화면에서 사용자가 동의 선택을 다시 하고 싶을 때. */
    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            if (formError != null) {
                Log.w(TAG, "동의 설정 화면을 띄우지 못했습니다: ${formError.message}")
            }
            syncConsentState(activity)
        }
    }

    private fun syncConsentState(context: Context) {
        val consentInformation = UserMessagingPlatform.getConsentInformation(context)
        privacyOptionsRequired = consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

        val allowed = consentInformation.canRequestAds()
        canRequestAds = allowed
        if (allowed) startSdk(context)
    }

    private fun startSdk(context: Context) {
        if (sdkStarted) return
        sdkStarted = true
        // 매니페스트의 OPTIMIZE_INITIALIZATION 플래그가 무거운 작업을
        // 백그라운드로 옮겨 줍니다. 결과를 기다릴 일이 없어 콜백은 비워 둡니다.
        MobileAds.initialize(context.applicationContext) {}
    }
}

/**
 * 전면 광고는 Activity 위에서만 띄울 수 있습니다. Compose 안에서는 Context 가
 * 래퍼로 감싸여 올 수 있어 한 겹씩 벗겨 찾습니다.
 */
internal fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
