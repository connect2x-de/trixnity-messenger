package de.connect2x.trixnity.messenger.compose.view.richtext

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.messenger.compose.view.files.rememberImageBitmapOrNull
import kotlinx.coroutines.CancellationException

private val log = Logger("de.connect2x.trixnity.messenger.compose.view.richtext.InlineImage")

internal val inlineImageDecodeSize = IntSize(800, 800)

private const val maxInlineImageDimension = 320f

private fun String?.imageDimension(): Float? = this?.toIntOrNull()?.takeIf { it > 0 }?.toFloat()

@Composable
internal fun rememberInlineImage(uri: String, loadImage: (suspend (String) -> ByteArray?)?): ImageBitmap? {
    val bytes by
        produceState<ByteArray?>(null, uri, loadImage) {
            value = null
            value = loadInlineImageOrNull(uri, loadImage)
        }
    return rememberImageBitmapOrNull(bytes, maxSize = inlineImageDecodeSize)
}

internal suspend fun loadInlineImageOrNull(uri: String, loadImage: (suspend (String) -> ByteArray?)?): ByteArray? =
    try {
        loadImage?.invoke(uri)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        log.warn { "Unable to load inline image: ${e.message}" }
        null
    }

internal fun CompoundTextContext.measureInlineImage(
    image: RichText.Inline.Block,
    bitmap: ImageBitmap?,
    constraints: Constraints,
): Placeholder? {
    if (bitmap == null) return null
    val maxWidth = with(density) { constraints.maxWidth.toDp().value }
    val size = inlineImageSize(image.attributes, IntSize(bitmap.width, bitmap.height), maxWidth)
    return with(density) {
        Placeholder(
            width = size.width.dp.toSp(),
            height = size.height.dp.toSp(),
            placeholderVerticalAlign = PlaceholderVerticalAlign.Bottom,
        )
    }
}

internal fun inlineImageSize(attributes: Map<String, String>, intrinsicSize: IntSize, maxWidth: Float): Size {
    val width = attributes["width"].imageDimension()
    val height = attributes["height"].imageDimension()
    val ratio = intrinsicSize.width.toFloat() / intrinsicSize.height
    val requestedHeight = height ?: width?.let { it / ratio } ?: intrinsicSize.height.toFloat()
    val requestedWidth = width ?: requestedHeight * ratio
    val boundedWidth = maxWidth.coerceIn(1f, maxInlineImageDimension)
    val scale = minOf(1f, boundedWidth / requestedWidth, maxInlineImageDimension / requestedHeight)
    return Size((requestedWidth * scale).coerceAtLeast(1f), (requestedHeight * scale).coerceAtLeast(1f))
}

@Composable
internal fun InlineImage(image: RichText.Inline.Block, bitmap: ImageBitmap) {
    val alt = image.attributes["alt"] ?: image.attributes["title"] ?: ""
    Image(bitmap, contentDescription = alt, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
}
