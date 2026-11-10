package de.connect2x.trixnity.messenger.compose.view.room.timeline.element.file

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.core.MSC2448
import de.connect2x.trixnity.messenger.compose.view.common.LoadingSpinner
import de.connect2x.trixnity.messenger.compose.view.files.rememberImageBitmapOrNull
import de.connect2x.trixnity.messenger.compose.view.util.BlurHashDecoder
import de.connect2x.trixnity.messenger.compose.view.util.animateImage
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.file.FileBasedTimelineElementViewModel

@OptIn(MSC2448::class)
@Composable
internal fun ImageTimelineElementView(
    element: FileBasedTimelineElementViewModel.Image,
    modifier: Modifier = Modifier,
    imageModifier: Modifier = Modifier.fillMaxSize(),
    contentDescription: String? = null,
) {
    val thumbnail = element.thumbnail.collectAsState().value
    val loading = element.thumbnailLoading.collectAsState().value
    val bitmap = rememberImageBitmapOrNull(thumbnail, IntSize(800, 800))
    val painter = remember(bitmap) { bitmap?.let(::BitmapPainter) }
    val blurhash = element.blurhash
    val size = timelineImageSize(element)
    val fallback =
        remember(blurhash, size) {
            if (blurhash != null && size != null) BlurHashDecoder.decode(blurhash, size)?.let(::BitmapPainter) else null
        }
    val imagePainter = animateImage(painter, fallback)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (imagePainter != null) {
            Image(
                painter = imagePainter,
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = imageModifier,
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(10.dp)) {
                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(64.dp))
                Text(element.name, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (loading) LoadingSpinner()
    }
}

internal fun Modifier.timelineImageAspectRatio(element: FileBasedTimelineElementViewModel.Image): Modifier {
    val size = timelineImageSize(element) ?: return this
    return aspectRatio(size.width.toFloat() / size.height.toFloat())
}

private fun timelineImageSize(element: FileBasedTimelineElementViewModel.Image): IntSize? {
    val thumbnailWidth = element.thumbnailWidth
    val thumbnailHeight = element.thumbnailHeight
    val width = element.width
    val height = element.height
    return when {
        thumbnailWidth != null && thumbnailWidth > 0 && thumbnailHeight != null && thumbnailHeight > 0 ->
            IntSize(thumbnailWidth, thumbnailHeight)
        width != null && width > 0 && height != null && height > 0 -> IntSize(width, height)
        else -> null
    }
}
