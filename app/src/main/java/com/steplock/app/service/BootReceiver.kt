package com.steplock.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.steplock.app.system.PermissionStep
import com.steplock.app.system.nextPermissionStep

/**
 * 재부팅하면 감시 서비스가 사라지므로 다시 띄웁니다.
 * BOOT_COMPLETED는 포그라운드 서비스 시작이 허용되는 예외라 여기서 바로 시작할 수 있습니다.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (nextPermissionStep(context) != PermissionStep.Ready) return
        runCatching { AppWatchService.start(context) }
    }
}
