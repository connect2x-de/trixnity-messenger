@file:JvmName("ImageDecodingImpl")

package de.connect2x.messenger.compose.view.util

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

actual suspend fun decodeImageRGBA8888(imageData: ByteArray, newWidth: Int, newHeight: Int): ByteArray? {
    return try {
        ByteArrayInputStream(imageData).use {
            // Decode raw media contents into a renderable image
            val decodedImage = ImageIO.read(it)
            val actualWidth = if (newWidth == -1) decodedImage.width else newWidth
            val actualHeight = if (newHeight == -1) decodedImage.height else newHeight
            // Re-render the image with a known, fixed pixel packing
            val transcodedImage = BufferedImage(actualWidth, actualHeight, BufferedImage.TYPE_INT_ARGB)
            val graphics = transcodedImage.createGraphics()
            graphics.drawImage(decodedImage, 0, 0, actualWidth, actualHeight, null)
            graphics.dispose()
            // Calculate buffer size and retrieve ARGB pixel data
            val iconPixelCount = actualWidth * actualHeight
            val rawPixelData = IntArray(iconPixelCount)
            transcodedImage.getRGB(0, 0, actualWidth, actualHeight, rawPixelData, 0, actualWidth)
            // Swizzle pixel data from ARGB to RGBA
            val iconDataSize = iconPixelCount shl 2
            val iconData = ByteArray(iconDataSize)
            for (i in rawPixelData.indices) {
                val pixel = rawPixelData[i]
                val componentIndex = i shl 2
                iconData[componentIndex] = ((pixel shr 16) and 0xFF).toByte()
                iconData[componentIndex + 1] = ((pixel shr 8) and 0xFF).toByte()
                iconData[componentIndex + 2] = (pixel and 0xFF).toByte()
                iconData[componentIndex + 3] = ((pixel shr 24) and 0xFF).toByte()
            }
            iconData
        }
    } catch (error: Throwable) {
        null
    }
}
