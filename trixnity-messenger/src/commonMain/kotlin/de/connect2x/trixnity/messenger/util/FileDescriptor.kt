package de.connect2x.trixnity.messenger.util

import io.ktor.http.*
import net.folivo.trixnity.utils.ByteArrayFlow

interface FileDescriptor {
    val fileName: String
    val fileSize: Long?
    val mimeType: ContentType?
    val content: ByteArrayFlow
}

data class BasicFileDescriptor(
    override val fileName: String,
    override val fileSize: Long?,
    override val mimeType: ContentType?,
    override val content: ByteArrayFlow
) : FileDescriptor
