package de.connect2x.messenger.compose.view.util

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import de.connect2x.messenger.compose.view.files.toImageBitmap

actual suspend fun decodeImageRGBA8888(
    imageData: ByteArray,
    newWidth: Int,
    newHeight: Int
): ByteArray? {
    var bitmap = imageData.toImageBitmap() ?: return null
    val actualWidth = if (newWidth == -1) bitmap.width else newWidth
    val actualHeight = if (newHeight == -1) bitmap.height else newHeight
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
    return iconData
}
