package com.steplock.app.service

import android.accessibilityservice.AccessibilityService
import android.content.pm.ApplicationInfo
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.steplock.app.data.ShortFormDetector
import com.steplock.app.data.ShortFormState
import com.steplock.app.data.ShortFormTarget

/**
 * 유튜브·인스타그램에서 **짧은 영상 화면이 떠 있는지만** 봅니다.
 *
 * 하는 일은 그게 전부입니다 — 잠금 화면은 감시 서비스(AppWatchService)가 기존 규칙
 * (조건 · 임시 허용 · 집중)대로 띄웁니다. 여기서는 [ShortFormState] 에 "보는 중 / 아님"만
 * 남깁니다.
 *
 * - 이벤트는 설정 파일(`res/xml/short_form_accessibility.xml`)에서 두 앱으로만 받습니다.
 *   다른 앱의 화면은 이 서비스에 오지 않습니다.
 * - 읽는 건 화면 구성 요소의 **이름(view id)** 뿐입니다. 글자·영상 내용은 읽지 않고,
 *   아무것도 저장하거나 보내지 않습니다.
 * - 영상이 재생되는 동안 화면 변경 이벤트가 쉴 새 없이 와서, [CHECK_INTERVAL_MS] 에
 *   한 번만 훑습니다. 건너뛴 이벤트가 마지막이었을 수 있어 한 번은 늦게라도 다시 봅니다.
 */
class ShortFormWatchService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var lastCheckAt = 0L
    private var pendingPackage: String? = null
    private var lastLogged: Set<String>? = null

    private val deferredCheck = Runnable {
        pendingPackage?.let { check(it) }
        pendingPackage = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return
        if (ShortFormTarget.of(packageName) == null) return
        val now = SystemClock.uptimeMillis()
        val windowChanged = event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        if (!windowChanged && now - lastCheckAt < CHECK_INTERVAL_MS) {
            if (pendingPackage == null) {
                pendingPackage = packageName
                handler.postDelayed(deferredCheck, CHECK_INTERVAL_MS - (now - lastCheckAt))
            }
            return
        }
        check(packageName)
    }

    private fun check(packageName: String) {
        val target = ShortFormTarget.of(packageName) ?: return
        lastCheckAt = SystemClock.uptimeMillis()
        val root = rootInActiveWindow ?: return
        // 앞에 있는 창이 다른 앱이면(알림창을 내렸다든지) 판단하지 않습니다.
        if (root.packageName?.toString() != packageName) return
        val ids = collectViewIds(root)
        ShortFormState.report(packageName, ShortFormDetector.isShortForm(target, ids.asSequence()))
        logForDebugBuild(packageName, ids)
    }

    /** 화면 트리를 넓이 우선으로 훑어 이름 있는 요소만 모읍니다. 너무 큰 화면은 앞부분만. */
    private fun collectViewIds(root: AccessibilityNodeInfo): Set<String> {
        val ids = mutableSetOf<String>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0
        while (queue.isNotEmpty() && visited < MAX_NODES) {
            val node = queue.removeFirst()
            visited++
            node.viewIdResourceName?.let { ids += it }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return ids
    }

    /**
     * 디버그 빌드에서만, 화면 구성이 바뀔 때 이름 목록을 로그로 남깁니다.
     * 유튜브 업데이트로 감지가 멈췄을 때 `adb logcat -s StepLockShortForm` 으로
     * 새 이름을 찾는 데 씁니다. 출시 빌드에는 남기지 않습니다.
     */
    private fun logForDebugBuild(packageName: String, ids: Set<String>) {
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0) return
        if (ids == lastLogged) return
        lastLogged = ids
        val names = ids.map { it.substringAfter(":id/") }.sorted()
        Log.d(TAG, "$packageName ${names.size}개: ${names.joinToString(", ")}")
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        handler.removeCallbacks(deferredCheck)
        super.onDestroy()
    }

    private companion object {
        const val TAG = "StepLockShortForm"
        const val CHECK_INTERVAL_MS = 300L
        const val MAX_NODES = 2_000
    }
}
