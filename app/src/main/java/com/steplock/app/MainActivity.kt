package com.steplock.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.steplock.app.navigation.StepLockNavHost
import com.steplock.app.service.AppWatchService
import com.steplock.app.system.PermissionStep
import com.steplock.app.system.nextPermissionStep
import com.steplock.app.ui.theme.StepLockTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StepLockTheme {
                StepLockNavHost()
            }
        }
    }

    /** 권한이 갖춰져 있으면 앱을 열 때마다 감시 서비스를 다시 살려 둡니다. */
    override fun onStart() {
        super.onStart()
        if (nextPermissionStep(this) == PermissionStep.Ready) {
            AppWatchService.start(this)
        }
    }
}
