package com.steplock.app.system

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
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

    /**
     * 알림 권한. Android 13 미만에서는 권한 자체가 없어 항상 허용으로 봅니다.
     *
     * 이걸 받지 않으면 감시 서비스와 집중 타이머 알림이 **보이지 않습니다.**
     * 서비스는 그대로 돌지만, 사용자는 감시가 켜져 있는지 알 방법이 없습니다.
     */
    fun hasNotifications(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    /**
     * 대화상자 하나로 함께 물을 수 있는 런타임 권한들.
     * Android 13 미만에서는 알림 권한이 없으므로 걸음 수만 남습니다.
     */
    fun runtimePermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.ACTIVITY_RECOGNITION, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            arrayOf(Manifest.permission.ACTIVITY_RECOGNITION)
        }

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

/** 홈에서 "권한이 꺼졌어요" 경고를 띄울 때 어느 것이 빠졌는지 가리킵니다. */
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

/**
 * 온보딩에 **한 화면으로 나열하는** 권한 묶음.
 *
 * 걸음 수와 알림은 런타임 권한이라 대화상자 **하나로 함께** 물을 수 있어서 한 줄로
 * 묶었습니다. 사용 정보 접근과 다른 앱 위에 표시는 안드로이드가 각각 다른 설정
 * 화면에서만 허용을 받게 해서, 세 줄을 한 번에 끝내는 방법은 없습니다 — 대신 세
 * 줄을 한 화면에 보여 주고 남은 개수를 알려 줍니다.
 */
enum class PermissionGroup(
    @StringRes val titleRes: Int,
    @StringRes val descRes: Int,
) {
    Runtime(R.string.permission_runtime_title, R.string.permission_runtime_desc),
    UsageAccess(R.string.permission_usage_title, R.string.permission_usage_desc),
    Overlay(R.string.permission_overlay_title, R.string.permission_overlay_desc),
    ;

    /** 설정 화면으로 나가야 하는 권한은 버튼 문구를 달리합니다. */
    val opensSettings: Boolean get() = this != Runtime
}

fun PermissionGroup.isGranted(context: Context): Boolean = when (this) {
    PermissionGroup.Runtime ->
        hasActivityRecognitionPermission(context) && AppPermissions.hasNotifications(context)
    PermissionGroup.UsageAccess -> AppPermissions.hasUsageAccess(context)
    PermissionGroup.Overlay -> AppPermissions.hasOverlay(context)
}

fun permissionStates(context: Context): Map<PermissionGroup, Boolean> =
    PermissionGroup.entries.associateWith { it.isGranted(context) }
