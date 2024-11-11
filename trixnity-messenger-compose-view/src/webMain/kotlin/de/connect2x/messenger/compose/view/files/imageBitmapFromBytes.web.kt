package de.connect2x.messenger.compose.view.files

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.skia.Image

private val log = KotlinLogging.logger { }

actual fun imageBitmapFromBytes(encodedImageData: ByteArray): ImageBitmap? {
    return try {
        Image.makeFromEncoded(encodedImageData).toComposeImageBitmap()
    } catch (e: Throwable) {
        log.error(e) { "Cannot decode image" }
        null
    }
}
