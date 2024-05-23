package de.connect2x.trixnity.messenger.util

import io.ktor.http.ContentType
import io.ktor.http.fromFilePath
import net.folivo.trixnity.utils.ByteArrayFlow
import net.folivo.trixnity.utils.byteArrayFlowFromReadableStream
import web.file.File

class FileDescriptorJS(
    private val file: File
) : FileDescriptor {
    override val fileName: String
        get() = file.name
    override val fileSize: Int
        get() = file.size.toInt()
    override val mimeType: ContentType?
        get() = ContentType.fromFilePath(file.name).firstOrNull()
    override val content: ByteArrayFlow
        get() = byteArrayFlowFromReadableStream { file.stream() }

}
