package de.connect2x.trixnity.messenger.compose.view.files

import androidx.compose.ui.graphics.ImageBitmap
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap

private val log = KotlinLogging.logger {}

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
