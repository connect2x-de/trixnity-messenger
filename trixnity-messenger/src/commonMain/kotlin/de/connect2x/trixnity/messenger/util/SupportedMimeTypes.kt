package de.connect2x.trixnity.messenger.util

import io.ktor.http.ContentType

object SupportedMimeTypes {
    private val supportedImageMimeTypes: List<ContentType> = listOf(
        ContentType.Image.JPEG,
        ContentType.Image.PNG,
        ContentType.Image.BMP,
        ContentType.Image.Webp,
        ContentType.Image.GIF // gifs can be rendered statically (first frame)
    )

    // JPEG, PNG, BMP, WEBP (based on decodeToImageBitmap())
    fun isSupportedImage(mimeType: ContentType): Boolean = supportedImageMimeTypes.any { it.match(mimeType) }
}
