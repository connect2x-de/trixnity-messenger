package de.connect2x.trixnity.messenger.util

import io.ktor.http.ContentType
import io.ktor.http.fromFilePath
import net.folivo.trixnity.utils.byteArrayFlowFromSource
import okio.FileSystem
import okio.Path

class PathFileDescriptor(
    val path: Path,
    private val fileSystem: FileSystem
) : FileDescriptor {

    override val fileName = path.name
    override val fileSize = fileSystem.metadataOrNull(path)?.size
    override val mimeType = ContentType.fromFilePath(fileName).firstOrNull()
    override val content = byteArrayFlowFromSource { fileSystem.source(path) }
}
