package de.connect2x.trixnity.messenger.util

import io.ktor.http.*
import kotlinx.serialization.KSerializer
import net.folivo.trixnity.utils.ByteArrayFlow
import org.koin.core.module.Module

data class FileInfo(
    val fileName: String,
    val fileSize: Int?,
    val mimeType: ContentType?,
    val content: ByteArrayFlow
)

fun interface GetFileInfo {
    suspend operator fun invoke(fileDescriptor: FileDescriptor): FileInfo?
}

expect class FileDescriptor

expect class FileDescriptorSerializer : KSerializer<FileDescriptor>

expect fun platformGetFileInfoModule(): Module