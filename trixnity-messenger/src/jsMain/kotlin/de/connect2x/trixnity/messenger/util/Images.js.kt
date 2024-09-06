package de.connect2x.trixnity.messenger.util

import io.ktor.http.ContentType
import korlibs.image.format.readBitmapInfo
import korlibs.io.file.std.asMemoryVfsFile
import net.folivo.trixnity.utils.ByteArrayFlow
import net.folivo.trixnity.utils.toByteArray

actual suspend fun getImageDimensions(byteArrayFlow: ByteArrayFlow): Pair<Int?, Int?> {
    // TODO this does not seem to work
    val bitmapInfo = byteArrayFlow.toByteArray().asMemoryVfsFile().readBitmapInfo()
    return bitmapInfo?.let { it.width to it.height } ?: (null to null)
}

actual suspend fun rotateImageToMetadataOrientation(imageBytes : ByteArray, mimeType: ContentType) : ByteArray {
    // TODO implement
    return imageBytes
}
