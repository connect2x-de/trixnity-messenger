package de.connect2x.messenger.compose.view.files

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger { }

actual fun imageBitmapFromBytes(encodedImageData: ByteArray): ImageBitmap? {
    val bitmap: Bitmap? = try {
        BitmapFactory.decodeByteArray(encodedImageData, 0, encodedImageData.size)
    } catch (e: Exception) {
        log.error(e) { "cannot create imageBitmapFromBytes" }
        null
    }
    return bitmap?.asImageBitmap()
}
