package com.steplock.app.data

import androidx.annotation.StringRes
import com.steplock.app.R

/**
 * 앱 전체가 아니라 **짧은 영상 화면만** 막을 수 있는 앱.
 *
 * 쇼츠·릴스는 앱 안의 한 화면이라 안드로이드 사용 기록(앱 단위)으로는 구분되지
 * 않습니다. 접근성 서비스가 그 앱의 화면 구성 요소 이름(view id)을 읽어서, 짧은 영상
 * 플레이어에만 있는 이름이 보이면 "지금 쇼츠를 보고 있다"고 판단합니다.
 *
 * **[viewIds] 는 각 앱이 업데이트하면 바뀔 수 있습니다.** 유튜브·인스타그램이 내부
 * 이름을 바꾸면 감지가 멈추므로, 바뀌면 이 목록만 고치면 되게 한곳에 모았습니다.
 * 아래 값은 실기기에서 확인하지 못한 **추정값**입니다 — README 의 "쇼츠만 막기"
 * 항목에 있는 명령으로 실제 화면 구조를 뽑아 맞춰야 합니다.
 */
enum class ShortFormTarget(
    val packageName: String,
    /** 잠금 화면 제목 등에 쓰는 이름 — "YouTube 쇼츠". */
    @StringRes val labelRes: Int,
    /** 선택지에 쓰는 짧은 이름 — "쇼츠만". */
    @StringRes val onlyRes: Int,
    /** 짧은 영상 화면에만 있는 구성 요소 이름(패키지 접두사 없이). */
    val viewIds: Set<String>,
) {
    YouTubeShorts(
        packageName = "com.google.android.youtube",
        labelRes = R.string.short_form_youtube,
        onlyRes = R.string.short_form_only_shorts,
        viewIds = setOf(
            "reel_recycler",
            "reel_player_page_container",
            "reel_watch_player",
        ),
    ),
    InstagramReels(
        packageName = "com.instagram.android",
        labelRes = R.string.short_form_instagram,
        onlyRes = R.string.short_form_only_reels,
        viewIds = setOf(
            "clips_viewer_view_pager",
            "clips_viewer_container",
            "clips_video_container",
        ),
    ),
    ;

    companion object {
        fun of(packageName: String?): ShortFormTarget? = entries.firstOrNull { it.packageName == packageName }

        val packageNames: Set<String> = entries.mapTo(mutableSetOf()) { it.packageName }
    }
}

object ShortFormDetector {
    /**
     * 화면에 있는 구성 요소 이름들 중 짧은 영상 화면의 표식이 있는지.
     *
     * 안드로이드가 주는 이름은 `com.google.android.youtube:id/reel_recycler` 꼴이라
     * `:id/` 뒤만 비교합니다. 이름이 없는 요소(빈 문자열)는 건너뜁니다.
     */
    fun isShortForm(target: ShortFormTarget, viewIds: Sequence<String>): Boolean =
        viewIds.any { id ->
            val name = id.substringAfter(":id/", missingDelimiterValue = "")
            name.isNotEmpty() && name in target.viewIds
        }
}

/**
 * 접근성 서비스가 마지막으로 본 짧은 영상 여부. 두 서비스가 같은 프로세스에서 돌아서
 * 메모리로 넘깁니다.
 *
 * 감시 서비스는 [isVisible] 에 **그 앱이 앞에 나온 시각**을 넘깁니다. 그보다 오래된
 * 값은 믿지 않습니다 — 잠금 화면이 덮여 있는 동안에는 접근성 이벤트가 오지 않아서,
 * 앱을 다시 열었을 때 지난번 "쇼츠 보는 중"이 남아 있으면 홈 화면인데도 잠깁니다.
 */
object ShortFormState {
    private data class Seen(val packageName: String, val visible: Boolean, val at: Long)

    @Volatile
    private var last: Seen? = null

    fun report(packageName: String, visible: Boolean, at: Long = System.currentTimeMillis()) {
        last = Seen(packageName, visible, at)
    }

    fun isVisible(packageName: String, since: Long): Boolean {
        val seen = last ?: return false
        return seen.packageName == packageName && seen.visible && seen.at >= since
    }
}
