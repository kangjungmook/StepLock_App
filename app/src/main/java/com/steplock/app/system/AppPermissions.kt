package com.steplock.app.system

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.annotation.StringRes
import com.steplock.app.R
import com.steplock.app.data.hasActivityRecognitionPermission

/**
 * 잠금 감시에 필요한 권한은 세 가지입니다.
 * 걸음 수는 런타임 권한, 사용 정보 접근과 화면 위 표시는 설정 화면으로 보내야 합니다.
 */
object AppPermissions {

    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun hasOverlay(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun usageAccessSettings(): Intent =
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun overlaySettings(context: Context): Intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.fromParts("package", context.packageName, null),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /**
     * 런타임 권한을 한 번 거절당한 뒤에는 다시 물어도 대화상자가 뜨지 않으므로,
     * 앱 정보 화면으로 보내 직접 켜게 합니다.
     */
    fun appDetailsSettings(context: Context): Intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

/** 온보딩 CTA는 아직 남은 권한 하나만 요구합니다. */
enum class PermissionStep(@StringRes val ctaRes: Int) {
    ActivityRecognition(R.string.permission_cta_activity),
    UsageAccess(R.string.permission_cta_usage),
    Overlay(R.string.permission_cta_overlay),
    Ready(R.string.permission_cta_start),
}

fun nextPermissionStep(context: Context): PermissionStep = when {
    !hasActivityRecognitionPermission(context) -> PermissionStep.ActivityRecognition
    !AppPermissions.hasUsageAccess(context) -> PermissionStep.UsageAccess
    !AppPermissions.hasOverlay(context) -> PermissionStep.Overlay
    else -> PermissionStep.Ready
}
