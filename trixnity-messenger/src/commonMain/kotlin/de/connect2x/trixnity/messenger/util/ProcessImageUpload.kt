package de.connect2x.trixnity.messenger.util

import io.ktor.http.*
import org.koin.core.module.Module

fun interface ProcessImageUpload {
    suspend operator fun invoke(imageBytes: ByteArray, mimeType: ContentType): ByteArray
}

expect fun platformProcessImageUploadModule(): Module
