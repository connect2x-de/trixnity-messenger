package de.connect2x.messenger.compose.view.util

expect suspend fun decodeImageRGBA8888(
    imageData: ByteArray,
    newWidth: Int = -1,
    newHeight: Int = -1
): ByteArray?
