package de.connect2x.trixnity.messenger.util

import io.ktor.http.*

object SupportedMimeTypes {
    private val supportedImageMimeTypes: List<ContentType> =
        listOf(
            ContentType.Image.JPEG,
            ContentType.Image.PNG,
            ContentType.Image.BMP,
            ContentType.Image.WEBP,
            ContentType.Image.GIF,
        )

    /**
     * Checks if the provided MIME type is a supported image format.
     *
     * Supported formats are JPEG, PNG, BMP, WebP, and GIF. Note that `decodeToImageBitmap` may only fully support JPEG,
     * PNG, BMP, and GIF (statically via the first frame).
     */
    fun isSupportedImage(mimeType: ContentType): Boolean = supportedImageMimeTypes.any { it.match(mimeType) }
}
