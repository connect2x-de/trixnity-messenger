@file:JvmName("ImageDecodingImpl")

package de.connect2x.messenger.compose.view.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.nio.ByteBuffer

actual suspend fun decodeImageRGBA8888(imageData: ByteArray, newWidth: Int, newHeight: Int): ByteArray? {
    return try {
        // Decode raw media contents into a renderable image
        val decodedImage = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
        val actualWidth = if (newWidth == -1) decodedImage.width else newWidth
        val actualHeight = if (newHeight == -1) decodedImage.height else newHeight
        // Re-render the image with a known, fixed pixel packing
        val transcodedImage = decodedImage.copy(Bitmap.Config.ARGB_8888, false)
        // Calculate buffer size and retrieve ARGB pixel data
        val iconPixelCount = actualWidth * actualHeight
        val rawPixelData = ByteBuffer.allocate(iconPixelCount)
        transcodedImage.copyPixelsToBuffer(rawPixelData)
        // Swizzle pixel data from ARGB to RGBA
        val iconDataSize = iconPixelCount shl 2
        val iconData = ByteArray(iconDataSize)
        val bufferView = rawPixelData.asIntBuffer()
        for (i in 0..<iconPixelCount) {
            val pixel = bufferView[i]
            val componentIndex = i shl 2
            iconData[componentIndex] = ((pixel shr 16) and 0xFF).toByte()
            iconData[componentIndex + 1] = ((pixel shr 8) and 0xFF).toByte()
            iconData[componentIndex + 2] = (pixel and 0xFF).toByte()
            iconData[componentIndex + 3] = ((pixel shr 24) and 0xFF).toByte()
        }
        iconData
    } catch (error: Throwable) {
        null
    }
}
