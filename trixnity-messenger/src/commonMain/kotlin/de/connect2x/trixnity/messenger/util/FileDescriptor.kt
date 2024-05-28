package de.connect2x.trixnity.messenger.util

import io.ktor.http.*
import net.folivo.trixnity.utils.ByteArrayFlow

interface FileDescriptor{
    val fileName: String
    val fileSize: Int?
    val mimeType: ContentType?
    val content: ByteArrayFlow
}
