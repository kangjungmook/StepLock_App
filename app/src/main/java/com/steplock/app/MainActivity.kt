package com.steplock.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.steplock.app.navigation.StepLockNavHost
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
}
