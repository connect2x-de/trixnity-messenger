package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.utils.ByteArrayFlow
import de.connect2x.trixnity.utils.byteArrayFlowFromReadableStream
import io.ktor.http.ContentType
import io.ktor.http.fromFilePath
import web.file.File
import web.url.URL

class JsFileDescriptor(private val file: File) : FileBackedFileDescriptor {
    override val fileName: String = file.name
    override val filePath: FilePath = FilePath(URL.createObjectURL(file))
    override val fileSize: Long? = file.size.toLong()
    override val mimeType: ContentType? = ContentType.fromFilePath(file.name).firstOrNull()
    override val content: ByteArrayFlow = byteArrayFlowFromReadableStream { file.stream() }
}

actual class FilePath(val url: String)
