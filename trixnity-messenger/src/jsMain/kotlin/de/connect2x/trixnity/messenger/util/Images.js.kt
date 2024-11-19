package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.viewmodel.util.limitedByteArrayOrNull
import korlibs.image.format.readBitmapInfo
import korlibs.io.file.std.asMemoryVfsFile
import net.folivo.trixnity.utils.ByteArrayFlow

actual suspend fun getImageDimensions(byteArrayFlow: ByteArrayFlow, maxMediaSize: Long): Pair<Int?, Int?> {
    // TODO this does not seem to work
    val bitmapInfo = byteArrayFlow.limitedByteArrayOrNull(maxMediaSize)?.asMemoryVfsFile()?.readBitmapInfo()
    return bitmapInfo?.let { it.width to it.height } ?: (null to null)
}

