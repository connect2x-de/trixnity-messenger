package de.connect2x.trixnity.messenger.compose.view.files

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.awt.Point
import java.awt.image.BufferedImage
import java.awt.image.ColorModel
import java.awt.image.DataBuffer
import java.awt.image.DataBufferInt
import java.awt.image.Raster
import java.awt.image.SinglePixelPackedSampleModel

actual inline fun createImageBitmap(width: Int, height: Int, crossinline drawPixel: (Int, Int) -> Color): ImageBitmap {
    val pixels = IntArray(width * height)
    for (y in 0 until height) {
        for (x in 0 until width) {
            pixels[y * width + x] = drawPixel(x, y).toArgb()
        }
    }
    val bitMasks = intArrayOf(0xFF0000, 0xFF00, 0xFF, 0xFF000000.toInt())
    val singlePixelPackedSampleModel = SinglePixelPackedSampleModel(DataBuffer.TYPE_INT, width, height, bitMasks)
    val dataBufferInt = DataBufferInt(pixels, pixels.size)
    val writableRaster = Raster.createWritableRaster(singlePixelPackedSampleModel, dataBufferInt, Point())
    val image = BufferedImage(ColorModel.getRGBdefault(), writableRaster, false, null)
    return image.toComposeImageBitmap()
}
