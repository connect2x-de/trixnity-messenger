package de.connect2x.trixnity.messenger.util

import io.ktor.http.ContentType
import net.folivo.trixnity.utils.ByteArrayFlow

// TODO find a way to get the image dimensions without loading the image into memory
expect suspend fun getImageDimensions(byteArrayFlow: ByteArrayFlow): Pair<Int?, Int?>

expect suspend fun rotateImageToMetadataOrientation(imageBytes : ByteArray, mimeType: ContentType) : ByteArray
