package com.steplock.app.system

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.steplock.app.service.ShortFormWatchService

/** "쇼츠만 막기"에 필요한 접근성 서비스가 켜져 있는지, 켜러 가는 길. */
object ShortFormAccess {

    /**
     * 이 설치 파일에 접근성 서비스가 들어 있는지. 폰에 바로 까는 디버그 APK 에는
     * 없습니다(Play 프로텍트가 설치를 막아서 — src/debug/AndroidManifest.xml).
     */
    fun isAvailable(context: Context): Boolean = runCatching {
        @Suppress("DEPRECATION")
        context.packageManager.getServiceInfo(ComponentName(context, ShortFormWatchService::class.java), 0)
    }.isSuccess

    fun isEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        val mine = ComponentName(context, ShortFormWatchService::class.java)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == mine }
    }

    /**
     * 접근성 설정. 서비스 하나를 바로 여는 공개 인텐트는 없어서 목록 화면으로 갑니다 —
     * 거기서 "설치된 앱" 아래 "스텝락 쇼츠 막기"를 누르면 됩니다.
     */
    fun settingsIntent(): Intent =
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
