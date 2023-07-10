package de.connect2x.trixnity.messenger.util

import korlibs.image.format.readBitmapInfo
import korlibs.io.file.std.asMemoryVfsFile

suspend fun getImageDimensions(byteArray: ByteArray): Pair<Int, Int> {
    val bitmapInfo = byteArray.asMemoryVfsFile().readBitmapInfo()
    return bitmapInfo?.let { it.width to it.height } ?: (0 to 0)
}