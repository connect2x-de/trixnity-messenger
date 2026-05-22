package de.connect2x.trixnity.messenger.util

import io.ktor.http.*
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformProcessImageUploadModule(): Module = module {
    single<ProcessImageUpload> {
        ProcessImageUpload { imageBytes, mimeType ->
            val rotated = rotateImageToMetadataOrientation(imageBytes, mimeType)
            removeImageMetadata(rotated)
        }
    }
}

/**
 * Rotates the data of an image to its Metadata orientation to prevent issues caused by missing interpretation of Exif
 * Data
 */
fun rotateImageToMetadataOrientation(imageBytes: ByteArray, mimeType: ContentType): ByteArray {
    // TODO implement
    return imageBytes
}
