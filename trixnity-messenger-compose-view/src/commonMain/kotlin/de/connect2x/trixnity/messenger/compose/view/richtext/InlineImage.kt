package de.connect2x.trixnity.messenger.compose.view.richtext

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.messenger.compose.view.files.rememberImageBitmapOrNull
import kotlinx.coroutines.CancellationException

private val log = Logger("de.connect2x.trixnity.messenger.compose.view.richtext.InlineImage")

internal const val inlineImageSize = 800
internal const val inlineEmoticonSize = 128
internal val LocalInlineImageMaxHeight = staticCompositionLocalOf<Dp?> { null }

private const val maxInlineImageDimension = 320f

private fun String?.imageDimension(): Float? = this?.toIntOrNull()?.takeIf { it > 0 }?.toFloat()

@Composable
internal fun rememberInlineImage(
    uri: String,
    width: Int,
    height: Int,
    loadImage: (suspend (String, Int, Int) -> ByteArray?)?,
): ImageBitmap? {
    val bytes by
        produceState<ByteArray?>(null, uri, width, height, loadImage) {
            value = null
            value = loadInlineImageOrNull { loadImage?.invoke(uri, width, height) }
        }
    return rememberImageBitmapOrNull(bytes, maxSize = IntSize(width, height))
}

internal suspend fun loadInlineImageOrNull(loadImage: (suspend () -> ByteArray?)): ByteArray? =
    try {
        loadImage()
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
    maxHeight: Float? = null,
): Placeholder? {
    if (bitmap == null) return null
    val fontSize = textStyle.fontSize.takeIf { it.isSp } ?: 14.sp
    val emoteHeight = with(density) { (fontSize * 2).toDp() }.value
    val maxWidth = with(density) { constraints.maxWidth.toDp().value }
    val size = inlineImageSize(image.attributes, IntSize(bitmap.width, bitmap.height), emoteHeight, maxWidth, maxHeight)
    return with(density) {
        Placeholder(
            width = size.width.dp.toSp(),
            height = size.height.dp.toSp(),
            placeholderVerticalAlign = PlaceholderVerticalAlign.Bottom,
        )
    }
}

internal fun inlineImageSize(
    attributes: Map<String, String>,
    intrinsicSize: IntSize,
    emoteHeight: Float,
    maxWidth: Float,
    maxHeight: Float? = null,
): Size {
    val emoticon = "data-mx-emoticon" in attributes
    val width = attributes["width"].imageDimension()
    val height = attributes["height"].imageDimension()
    val ratio = intrinsicSize.width.toFloat() / intrinsicSize.height
    val requestedHeight =
        if (emoticon) emoteHeight else height ?: width?.let { it / ratio } ?: intrinsicSize.height.toFloat()
    val requestedWidth =
        if (emoticon || maxHeight != null) requestedHeight * ratio else width ?: requestedHeight * ratio
    val boundedWidth = maxWidth.coerceIn(1f, maxInlineImageDimension)
    val boundedHeight = maxHeight?.coerceIn(1f, maxInlineImageDimension) ?: maxInlineImageDimension
    val scale = minOf(1f, boundedWidth / requestedWidth, boundedHeight / requestedHeight)
    return Size((requestedWidth * scale).coerceAtLeast(1f), (requestedHeight * scale).coerceAtLeast(1f))
}

@Composable
internal fun InlineImage(image: RichText.Inline.Block, bitmap: ImageBitmap) {
    val alt = image.attributes["alt"] ?: image.attributes["title"] ?: ""
    Image(bitmap, contentDescription = alt, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
}
