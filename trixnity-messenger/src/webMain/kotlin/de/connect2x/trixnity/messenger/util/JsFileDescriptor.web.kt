package de.connect2x.trixnity.messenger.util

import io.ktor.http.ContentType
import io.ktor.http.fromFilePath
import de.connect2x.trixnity.utils.ByteArrayFlow
import de.connect2x.trixnity.utils.byteArrayFlowFromReadableStream
import web.file.File

class JsFileDescriptor(val file: File) : FileDescriptor {

    override val fileName: String = file.name
    override val fileSize: Long = file.size.toLong()
    override val mimeType: ContentType? = ContentType.fromFilePath(file.name).firstOrNull()
    override val content: ByteArrayFlow = byteArrayFlowFromReadableStream { file.stream() }
}
