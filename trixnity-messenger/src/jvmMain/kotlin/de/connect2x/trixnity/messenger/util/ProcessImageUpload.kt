package de.connect2x.trixnity.messenger.util

import com.ashampoo.kim.Kim
import com.ashampoo.kim.format.tiff.constant.TiffTag
import com.ashampoo.kim.model.TiffOrientation
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.ContentType
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.koin.core.module.Module
import org.koin.dsl.module

private val log = KotlinLogging.logger { }

actual fun platformProcessImageUploadModule(): Module = module {
    single<ProcessImageUpload> {
        ProcessImageUpload { imageBytes, mimeType ->
            rotateImageToMetadataOrientation(imageBytes, mimeType)
        }
    }
}

/**
 * Rotates the data of an image to its Metadata orientation to prevent issues caused by missing interpretation
 * of Exif Data
 */
suspend fun rotateImageToMetadataOrientation(imageBytes: ByteArray, mimeType: ContentType): ByteArray {
    val metadata = Kim.readMetadata(imageBytes)
    val degrees = when (metadata?.findShortValue(TiffTag.TIFF_TAG_ORIENTATION)) {
        TiffOrientation.ROTATE_RIGHT.value.toShort() -> 90
        TiffOrientation.ROTATE_LEFT.value.toShort() -> 270
        TiffOrientation.UPSIDE_DOWN.value.toShort() -> 180
        else -> 0
    }
    if (degrees != 0) {
        try {
            val image = Image.makeFromEncoded(imageBytes)
            val bitmap = Bitmap.makeFromImage(image)
            log.debug { "Rotating image by $degrees degrees" }
            val encoded = Image.makeFromBitmap(bitmap).encodeToData(EncodedImageFormat.PNG)
            //Not needed currently since Metadata isn't sent. Might have to be reenabled later, if Metadata sending works to prevent sending of wrong metadata information
            /* val updatedBytes = if (encoded != null) {
            Kim.update(encoded.bytes, MetadataUpdate.Orientation(TiffOrientation.STANDARD))
        }
        else imageBytes*/
            return encoded?.bytes ?: imageBytes
        } catch (_: Exception) {
            return imageBytes
        }
    } else return imageBytes
}
