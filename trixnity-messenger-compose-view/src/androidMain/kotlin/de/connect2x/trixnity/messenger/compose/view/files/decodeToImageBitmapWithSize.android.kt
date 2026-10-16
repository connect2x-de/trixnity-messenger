package de.connect2x.trixnity.messenger.compose.view.files

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntSize
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi

@TrixnityMessengerPrivateApi
internal actual fun ByteArray.decodeToImageBitmapWithSize(maxSize: IntSize): ImageBitmap {
    checkDecodeToImageBitmapWithSizePreconditions(byteArray = this, maxSize = maxSize)

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }

    BitmapFactory.decodeByteArray(this, 0, size, bounds)

    validateActualImageSize(width = bounds.outWidth, height = bounds.outHeight)

    val sampleSize =
        calculateInSampleSize(
            width = bounds.outWidth,
            height = bounds.outHeight,
            maxWidth = maxSize.width,
            maxHeight = maxSize.height,
        )

    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }

    return BitmapFactory.decodeByteArray(this, 0, size, options).asImageBitmap()
}
