package de.connect2x.trixnity.messenger.util

import korlibs.image.format.readBitmapInfo
import korlibs.io.file.std.asMemoryVfsFile

// TODO find a way to get the image dimensions without loading the image into memory
suspend fun getImageDimensions(byteArray: ByteArray): Pair<Int?, Int?> {
    val bitmapInfo = byteArray.asMemoryVfsFile().readBitmapInfo()
    return bitmapInfo?.let { it.width to it.height } ?: (null to null)
}
