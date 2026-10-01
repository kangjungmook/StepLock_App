package com.steplock.app.system

import android.content.Context
import android.content.Intent
import com.steplock.app.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 설정 화면에서 허용 스위치를 켜는 순간 스텝락으로 **저절로 돌아오게** 합니다.
 *
 * 사용 정보 접근과 다른 앱 위에 표시는 안드로이드가 설정 화면에서만 켜게 해서, 앱이
 * 대신 켜 줄 방법은 없습니다. 대신 켠 뒤 "뒤로 가기를 몇 번 눌러 앱을 찾아오는" 수고를
 * 덜어 줍니다 — 0.5초마다 허용됐는지 보고, 허용되면 스텝락을 앞으로 가져옵니다.
 *
 * 백그라운드에서 화면을 띄우는 건 안드로이드 10+ 가 막는데, **다른 앱 위에 표시를
 * 받은 앱은 예외**입니다. 그래서 온보딩은 그 권한을 먼저 묻습니다. 그 권한이 없을 때는
 * 돌아오기가 막힐 수 있고, 그때는 사용자가 뒤로 가기로 돌아오면 됩니다.
 */
object PermissionReturn {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null

    /** 설정 화면을 열기 직전에 부릅니다. [isGranted] 가 참이 되면 앱을 앞으로 가져옵니다. */
    fun watch(context: Context, isGranted: (Context) -> Boolean) {
        val app = context.applicationContext
        job?.cancel()
        job = scope.launch {
            val deadline = System.currentTimeMillis() + WATCH_MS
            while (System.currentTimeMillis() < deadline) {
                delay(POLL_MS)
                if (isGranted(app)) {
                    bringBack(app)
                    return@launch
                }
            }
        }
    }

    /** 사용자가 직접 돌아왔으면 더 볼 필요가 없습니다. */
    fun cancel() {
        job?.cancel()
        job = null
    }

    private fun bringBack(context: Context) {
        runCatching {
            context.startActivity(
                Intent(context, MainActivity::class.java).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP,
                ),
            )
        }
    }

    private const val POLL_MS = 500L

    /** 이만큼 지나도 켜지 않으면 그만 봅니다 — 설정에서 다른 일을 하는 중일 수 있습니다. */
    private const val WATCH_MS = 3 * 60_000L
}
