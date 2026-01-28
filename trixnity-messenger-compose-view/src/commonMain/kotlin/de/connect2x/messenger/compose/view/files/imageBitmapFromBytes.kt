package de.connect2x.messenger.compose.view.files

import androidx.compose.ui.graphics.ImageBitmap
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.lognity.api.logger.error
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap

private val log: Logger = Logger("de.connect2x.messenger.compose.view.files.imageBitmapFromBytesKt")

expect fun ByteArray.toImageBitmap(width: Int, height: Int): ImageBitmap?

@OptIn(ExperimentalResourceApi::class)
fun ByteArray.toImageBitmap(): ImageBitmap? {
    return try {
        decodeToImageBitmap()
    } catch (e: Exception) {
        log.error(e) { "Cannot decode image" }
        null
    }

}
