package de.connect2x.trixnity.messenger.compose.view.files

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.unit.IntSize
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Codec
import org.jetbrains.skia.Data
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.impl.use

@TrixnityMessengerPrivateApi
internal actual fun ByteArray.decodeToImageBitmapWithSize(maxSize: IntSize): ImageBitmap {
    checkDecodeToImageBitmapWithSizePreconditions(byteArray = this, maxSize = maxSize)

    return Data.makeFromBytes(bytes = this).use { data ->
        Codec.makeFromData(data = data).use { codec ->
            val sourceInfo = codec.imageInfo

            validateActualImageSize(width = sourceInfo.width, height = sourceInfo.height)

            val sampleSize =
                calculateInSampleSize(
                    width = sourceInfo.width,
                    height = sourceInfo.height,
                    maxWidth = maxSize.width,
                    maxHeight = maxSize.height,
                )

            val targetInfo =
                ImageInfo.makeN32Premul(
                    width = (sourceInfo.width / sampleSize).coerceAtLeast(1),
                    height = (sourceInfo.height / sampleSize).coerceAtLeast(1),
                )

            val bitmap = Bitmap()
            bitmap.allocPixels(imageInfo = targetInfo)

            codec.readPixels(bitmap = bitmap)

            bitmap.asComposeImageBitmap()
        }
    }
}
