package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.utils.ByteArrayFlow
import de.connect2x.trixnity.utils.byteArrayFlowFromSource
import io.ktor.http.ContentType
import io.ktor.http.fromFilePath
import okio.FileSystem
import okio.Path

actual class InMemoryFileDescriptor(
    actual override val fileName: String,
    actual override val fileSize: Long?,
    actual override val mimeType: ContentType?,
    actual override val content: ByteArrayFlow,
) : FileDescriptor

actual class FileBackedFileDescriptor(val path: Path, private val fileSystem: FileSystem) : FileDescriptor {

    actual override val fileName = path.name
    actual val filePath = path.toString()
    actual override val fileSize = fileSystem.metadataOrNull(path)?.size
    actual override val mimeType = ContentType.fromFilePath(fileName).firstOrNull()
    actual override val content = byteArrayFlowFromSource { fileSystem.source(path) }
}
