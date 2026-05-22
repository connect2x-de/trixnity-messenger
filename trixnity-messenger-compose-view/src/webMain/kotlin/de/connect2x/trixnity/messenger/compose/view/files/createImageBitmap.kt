package de.connect2x.trixnity.messenger.compose.view.files

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.toArgb
import org.jetbrains.skia.Bitmap

actual inline fun createImageBitmap(width: Int, height: Int, crossinline drawPixel: (Int, Int) -> Color): ImageBitmap {
    return Bitmap()
        .apply {
            val data =
                ByteArray(height * width * 4).apply {
                    for (y in 0 until height) {
                        for (x in 0 until width) {
                            val index = y * width + x * 4
                            val color = drawPixel(x, y).toArgb()
                            set(index + 0, color.shr(24).and(0xFF).toByte())
                            set(index + 1, color.shr(16).and(0xFF).toByte())
                            set(index + 2, color.shr(8).and(0xFF).toByte())
                            set(index + 3, color.shr(0).and(0xFF).toByte())
                        }
                    }
                }
            installPixels(data)
        }
        .asComposeImageBitmap()
}
