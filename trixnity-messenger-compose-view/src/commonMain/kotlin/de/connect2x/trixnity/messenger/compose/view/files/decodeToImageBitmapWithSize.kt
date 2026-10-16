package de.connect2x.trixnity.messenger.compose.view.files

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntSize

internal expect fun ByteArray.decodeToImageBitmapWithSize(maxSize: IntSize): ImageBitmap

internal fun checkDecodeToImageBitmapWithSizePreconditions(byteArray: ByteArray, maxSize: IntSize) {
    require(byteArray.isNotEmpty()) { "Empty ByteArray is never a decodable image" }
    require(maxSize.width > 0 && maxSize.height > 0) {
        "Requested image dimensions (${maxSize.width}, ${maxSize.height}) are invalid"
    }
}

internal fun validateActualImageSize(width: Int, height: Int) {
    check(width > 0 && height > 0) { "Actual image dimensions ($width, $height) are invalid" }
}

internal fun calculateInSampleSize(width: Int, height: Int, maxWidth: Int, maxHeight: Int): Int {
    var sampleSize = 1

    while (width / (sampleSize * 2) >= maxWidth || height / (sampleSize * 2) >= maxHeight) {
        sampleSize *= 2
    }
    return sampleSize
}
