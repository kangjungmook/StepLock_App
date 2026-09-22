package com.steplock.app.ads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.steplock.app.BuildConfig
import com.steplock.app.ui.theme.SlColor

/**
 * 화면 아래에 붙는 배너.
 *
 * **통계 화면에만 둡니다.** 통계는 들여다보는 화면이라 광고가 가로막는 작업이 없습니다.
 * 잠금 화면·집중 타이머·온보딩에는 넣지 않습니다 — 그쪽은 사용자가 무언가를
 * 참거나 집중하는 중이라, 광고가 앱이 하려는 일을 그대로 방해합니다.
 *
 * 앵커드 어댑티브 크기를 쓰기 때문에 기기 폭에 맞는 높이를 SDK가 정해 줍니다.
 * 고정 320x50 을 쓰면 넓은 화면에서 양옆이 비어 어색해집니다.
 */
@Composable
fun SlBannerAd(
    modifier: Modifier = Modifier,
    unitId: String = BuildConfig.ADMOB_BANNER_UNIT,
) {
    // 동의가 확인되기 전에는 자리도 잡지 않습니다. 빈 칸을 미리 띄워 두면
    // 광고가 뜨는 순간 내용이 밀려 올라갑니다.
    if (!Ads.canRequestAds) return

    val context = LocalContext.current

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val widthDp = maxWidth.value.toInt()
        if (widthDp <= 0) return@BoxWithConstraints

        val adSize = remember(widthDp) {
            AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            // 내용과 광고의 경계를 한 줄로 분명히 합니다 — 광고가 앱 화면의
            // 일부로 읽히면 안 됩니다.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(SlColor.Border),
            )
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(adSize.height.dp)
                    .background(SlColor.Background),
                factory = { viewContext ->
                    AdView(viewContext).apply {
                        adUnitId = unitId
                        setAdSize(adSize)
                        loadAd(AdRequest.Builder().build())
                    }
                },
                // 화면을 떠날 때 뷰를 놓아 주지 않으면 배너가 계속 새 광고를
                // 받아 오면서 메모리에 남습니다.
                onRelease = { adView -> adView.destroy() },
            )
        }
    }
}
