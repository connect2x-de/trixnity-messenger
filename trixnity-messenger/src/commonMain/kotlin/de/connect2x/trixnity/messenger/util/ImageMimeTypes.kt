package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import io.ktor.http.ContentType
import io.ktor.http.fileExtensions

@TrixnityMessengerPrivateApi
object ImageMimeTypes {
    fun preferredFileExtension(contentType: ContentType): String? {
        val type = contentType.withoutParameters()
        return when (type) {
            ContentType.Image.JPEG -> "jpg"
            ContentType.Image.PNG -> "png"
            ContentType.Image.GIF -> "gif"
            ContentType.Image.WEBP -> "webp"
            ContentType.Image.BMP -> "bmp"
            ContentType.Image.AVIF -> "avif"
            else -> type.fileExtensions().firstOrNull()
        }
    }
}
