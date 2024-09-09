package de.connect2x.trixnity.messenger.util

import io.ktor.http.ContentType
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformProcessImageUploadModule(): Module = module {
    single<ProcessImageUpload> {
        ProcessImageUpload { imageBytes, mimeType ->
            rotateImageToMetadataOrientation(imageBytes, mimeType)
        }
    }
}

suspend fun rotateImageToMetadataOrientation(imageBytes: ByteArray, mimeType: ContentType): ByteArray {
    // TODO implement
    return imageBytes
}
