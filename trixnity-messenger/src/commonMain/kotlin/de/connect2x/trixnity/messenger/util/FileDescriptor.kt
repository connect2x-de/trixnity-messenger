package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.utils.ByteArrayFlow
import io.ktor.http.ContentType

interface FileDescriptor {
    val fileName: String
    val fileSize: Long?
    val mimeType: ContentType?
    val content: ByteArrayFlow
}

class BasicFileDescriptor(
    override val fileName: String,
    override val fileSize: Long?,
    override val mimeType: ContentType?,
    override val content: ByteArrayFlow,
) : FileDescriptor

interface FileBackedFileDescriptor : FileDescriptor {
    override val fileName: String
    val filePath: FilePath
    override val fileSize: Long?
    override val mimeType: ContentType?
    override val content: ByteArrayFlow
}

expect class FilePath
