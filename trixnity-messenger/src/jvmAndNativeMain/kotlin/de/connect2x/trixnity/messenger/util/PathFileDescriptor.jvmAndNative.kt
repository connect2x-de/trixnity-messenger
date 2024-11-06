package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.viewmodel.util.limitSize
import io.ktor.http.ContentType
import io.ktor.http.fromFilePath
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import net.folivo.trixnity.utils.byteArrayFlowFromSource
import okio.FileSystem
import okio.Path

class PathFileDescriptor(
    val path: Path,
    private val fileSystem: FileSystem,
    sizeLimit: Long? = null
) : FileDescriptor {

    override val fileName = path.name
    override val fileSize = fileSystem.metadataOrNull(path)?.size
    override val mimeType = ContentType.fromFilePath(fileName).firstOrNull()
    override val content =
        if (sizeLimit != null) byteArrayFlowFromSource { fileSystem.source(path) }.limitSize(sizeLimit)
        else byteArrayFlowFromSource {
            fileSystem.source(path)
        }
}
