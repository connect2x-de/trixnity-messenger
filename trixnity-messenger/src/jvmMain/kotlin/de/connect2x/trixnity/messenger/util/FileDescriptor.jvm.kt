package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.utils.byteArrayFlowFromSource
import io.ktor.http.ContentType
import io.ktor.http.fromFilePath
import okio.FileSystem
import okio.Path

class PathFileDescriptor(private val path: Path, private val fileSystem: FileSystem) : FileBackedFileDescriptor {

    override val fileName = path.name
    override val filePath = FilePath(path)
    override val fileSize = fileSystem.metadataOrNull(path)?.size
    override val mimeType = ContentType.fromFilePath(fileName).firstOrNull()
    override val content = byteArrayFlowFromSource { fileSystem.source(path) }
}

actual class FilePath(val path: Path)
