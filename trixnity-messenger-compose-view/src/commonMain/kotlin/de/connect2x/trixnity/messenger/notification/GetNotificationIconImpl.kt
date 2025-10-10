package de.connect2x.trixnity.messenger.notification

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

import de.connect2x.sysnotify.NotificationIcon
import de.connect2x.trixnity.messenger.compose.view.files.toImageBitmap
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger { }

// TODO this should be part of the SDK (e.g. using imagemagick) instead of view level
class GetNotificationIconImpl : GetNotificationIcon {
    override suspend fun invoke(encoded: ByteArray, maxWidth: Int, maxHeight: Int): NotificationIcon? {
        try {
            var bitmap = encoded.toImageBitmap() ?: return null
            val actualWidth = if (maxWidth < 0) bitmap.width else maxWidth
            val actualHeight = if (maxHeight < 0) bitmap.height else maxHeight
            // Re-scale image if input size doesn't match requested size
            if (bitmap.width != actualWidth || bitmap.height != actualHeight) {
                val newBitmap = ImageBitmap(actualWidth, actualHeight)
                val canvas = Canvas(newBitmap)
                canvas.drawImageRect(
                    image = bitmap,
                    srcOffset = IntOffset(0, 0),
                    srcSize = IntSize(bitmap.width, bitmap.height),
                    dstOffset = IntOffset(0, 0),
                    dstSize = IntSize(actualWidth, actualHeight),
                    paint = Paint()
                )
                bitmap = newBitmap
            }
            // Retrieve ARGB pixel data
            val pixelCount = actualWidth * actualHeight
            val pixels = bitmap.toPixelMap(0, 0, actualWidth, actualHeight).buffer
            val iconData = ByteArray(pixelCount shl 2)
            // We need to swizzle the color channels from ARGB to RGBA
            for (i in pixels.indices) {
                val pixel = pixels[i]
                val componentIndex = i shl 2
                iconData[componentIndex] = ((pixel shr 16) and 0xFF).toByte()
                iconData[componentIndex + 1] = ((pixel shr 8) and 0xFF).toByte()
                iconData[componentIndex + 2] = (pixel and 0xFF).toByte()
                iconData[componentIndex + 3] = ((pixel shr 24) and 0xFF).toByte()
            }
            return iconData.let { NotificationIcon(data = it, width = actualWidth, height = actualHeight) }
        } catch (e: Exception) {
            log.error(e) { "error while creating notification icon" }
            return null
        }
    }
}
