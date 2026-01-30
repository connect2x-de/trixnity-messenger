package de.connect2x.trixnity.messenger.compose.view.files

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.lognity.api.logger.error
import org.jetbrains.compose.resources.ExperimentalResourceApi

private val log: Logger = Logger("de.connect2x.trixnity.messenger.compose.view.files.imageBitmapFromBytesKt")

@OptIn(ExperimentalResourceApi::class)
actual fun ByteArray.toImageBitmap(
    width: Int,
    height: Int
): ImageBitmap? {
    return try {
        // Determine full image width/height
        val metadataOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(this, 0, size, metadataOptions)
        // Calculate downsampling parameters
        val decodeOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = false
            inSampleSize = 1
            if (metadataOptions.outHeight > height || width > metadataOptions.outWidth) {
                val halfHeight: Int = metadataOptions.outHeight / 2
                val halfWidth: Int = metadataOptions.outWidth / 2
                while (halfHeight / inSampleSize >= height && halfWidth / inSampleSize >= width) {
                    inSampleSize *= 2
                }
            }
        }
        // Decode downsampled image
        return BitmapFactory.decodeByteArray(this, 0, size, decodeOptions).asImageBitmap()
    } catch (e: Exception) {
        log.error(e) { "Cannot decode image" }
        null
    }
}
