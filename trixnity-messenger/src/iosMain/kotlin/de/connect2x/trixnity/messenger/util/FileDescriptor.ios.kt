package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.utils.BYTE_ARRAY_FLOW_CHUNK_SIZE
import de.connect2x.trixnity.utils.ByteArrayFlow
import de.connect2x.trixnity.utils.toByteArrayFlow
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.size
import io.github.vinceglb.filekit.source
import io.github.vinceglb.filekit.utils.toPath
import io.ktor.http.ContentType
import io.ktor.http.fromFilePath
import kotlinx.coroutines.flow.flow
import kotlinx.io.InternalIoApi
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.readByteArray

class ByteArrayFileDescriptor(private val path: Path, private val data: ByteArray) : InMemoryFileDescriptor {
    override val fileName: String = path.name
    override val fileSize: Long? = data.size.toLong()
    override val mimeType: ContentType? = ContentType.fromFilePath(fileName).firstOrNull()
    override val content: ByteArrayFlow = data.toByteArrayFlow()
}

class FileKitFileDescriptor(internal val file: PlatformFile) : FileBackedFileDescriptor {

    override val fileName: String = file.path.toPath().name
    override val filePath: String = file.path
    override val fileSize: Long? = file.size()
    override val mimeType: ContentType? = ContentType.fromFilePath(fileName).firstOrNull()

    @OptIn(InternalIoApi::class)
    override val content: ByteArrayFlow = flow {
        file.source().buffered().use { source ->
            while (!source.exhausted()) {
                emit(source.readByteArray(BYTE_ARRAY_FLOW_CHUNK_SIZE.coerceAtMost(source.buffer.size).toInt()))
            }
        }
    }
}
