package de.connect2x.trixnity.messenger.compose.view.files

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val log = Logger("de.connect2x.trixnity.messenger.compose.view.files.decodeToImageBitmapOrNull")

/**
 * Decodes an encoded image, such as PNG or JPEG.
 *
 * Returns null for empty, unsupported, or malformed data.
 */
@TrixnityMessengerPrivateApi
fun ByteArray.decodeToImageBitmapOrNull(): ImageBitmap? {
    if (isEmpty()) return null

    return try {
        decodeToImageBitmap().takeIf { it.width > 0 && it.height > 0 }
    } catch (e: Exception) {
        log.warn { "Unable to decode Image: ${e.message}" }
        null
    }
}

@TrixnityMessengerPrivateApi
@Composable
fun rememberImageBitmapOrNull(bytes: ByteArray?): ImageBitmap? {
    if (bytes == null) return null

    val bitmap by
        produceState<ImageBitmap?>(initialValue = null, key1 = bytes) {
            value = withContext(Dispatchers.Default) { bytes.decodeToImageBitmapOrNull() }
        }

    return bitmap
}
