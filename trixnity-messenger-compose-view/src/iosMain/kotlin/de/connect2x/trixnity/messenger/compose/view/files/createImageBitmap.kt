package de.connect2x.trixnity.messenger.compose.view.files

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo

actual inline fun createImageBitmap(width: Int, height: Int, crossinline drawPixel: (Int, Int) -> Color): ImageBitmap {
    val bytes = ByteArray((width * height) shl 2)
    val imageInfo = ImageInfo.makeN32Premul(width, height)
    for (y in 0 until height) {
        for (x in 0 until width) {
            val color = drawPixel(x, y)
            val argb = color.toArgb()
            val index = (y * width + x) shl 2
            bytes[index + 0] = ((argb shr 16) and 0xFF).toByte() // R
            bytes[index + 1] = ((argb shr 8) and 0xFF).toByte() // G
            bytes[index + 2] = ((argb shr 0) and 0xFF).toByte() // B
            bytes[index + 3] = ((argb shr 24) and 0xFF).toByte() // A
        }
    }
    val image = Image.makeRaster(imageInfo, bytes, width shl 2)
    return image.toComposeImageBitmap()
}
