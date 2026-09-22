package com.steplock.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.steplock.app.data.InstalledAppsRepository
import com.steplock.app.ui.theme.SlColor
import com.steplock.app.ui.theme.SlDimen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 기기에서 읽은 앱 아이콘.
 *
 * 잠글 앱을 고르는 화면에서는 이름만으로 앱을 알아보기 어렵습니다 — "라이트",
 * "Lite", "Go" 같은 변종이 많아서 아이콘이 있어야 구분됩니다.
 *
 * `PackageManager` 조회와 비트맵 변환은 목록을 스크롤하는 동안 여러 번 일어나므로
 * IO 디스패처에서 하고, 준비되기 전에는 빈 자리를 같은 크기로 잡아 둡니다 —
 * 나중에 채워지며 줄 높이가 흔들리지 않게.
 *
 * 아이콘을 못 읽으면(지워진 앱) 앱 이름 첫 글자로 [AppBadge] 를 대신 그립니다.
 */
@Composable
fun AppIcon(
    packageName: String,
    label: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val context = LocalContext.current
    val sizePx = with(LocalDensity.current) { size.roundToPx() }

    val icon by produceState<ImageBitmap?>(initialValue = null, packageName, sizePx) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                InstalledAppsRepository(context)
                    .icon(packageName)
                    ?.toBitmap(width = sizePx, height = sizePx)
                    ?.asImageBitmap()
            }.getOrNull()
        }
    }

    val bitmap = icon
    when {
        bitmap != null -> Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(SlDimen.RadiusBadgeSmall)),
        )

        else -> Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(SlDimen.RadiusBadgeSmall)),
            contentAlignment = Alignment.Center,
        ) {
            AppBadge(
                initial = label.take(1).uppercase().ifEmpty { "?" },
                color = SlColor.BorderStrong,
                size = size,
            )
        }
    }
}
