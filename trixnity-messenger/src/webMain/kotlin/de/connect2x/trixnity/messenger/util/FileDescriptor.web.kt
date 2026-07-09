package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.utils.ByteArrayFlow
import de.connect2x.trixnity.utils.byteArrayFlowFromReadableStream
import de.connect2x.trixnity.utils.byteArrayFlowFromSource
import io.ktor.http.ContentType
import io.ktor.http.fromFilePath
import okio.Buffer
import web.file.File

@Deprecated(
    message = "InMemoryFileDescriptor is not supported on this platform.",
    level = DeprecationLevel.ERROR,
)
actual class InMemoryFileDescriptor : FileDescriptor {
    actual override val fileName: String = ""
    actual override val fileSize: Long? = null
    actual override val mimeType: ContentType? = null
    actual override val content: ByteArrayFlow = byteArrayFlowFromSource { Buffer() }
}

actual class FileBackedFileDescriptor(val file: File) : FileDescriptor {
    actual override val fileName: String = file.name
    actual val filePath: String = file.webkitRelativePath
    actual override val fileSize: Long? = file.size.toLong()
    actual override val mimeType: ContentType? = ContentType.fromFilePath(file.name).firstOrNull()
    actual override val content: ByteArrayFlow = byteArrayFlowFromReadableStream { file.stream() }
}
