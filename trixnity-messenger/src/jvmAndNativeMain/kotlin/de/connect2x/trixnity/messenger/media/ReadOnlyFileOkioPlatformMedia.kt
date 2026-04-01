package de.connect2x.trixnity.messenger.media

import de.connect2x.trixnity.client.media.okio.OkioPlatformMedia
import de.connect2x.trixnity.utils.ByteArrayFlow
import de.connect2x.trixnity.utils.readByteArrayFlow
import de.connect2x.trixnity.utils.toByteArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.FlowCollector
import okio.FileSystem
import okio.IOException
import okio.Path

class ReadOnlyFileOkioPlatformMedia(val path: Path, val fileSystem: FileSystem) : OkioPlatformMedia {
    override fun transformByteArrayFlow(transformer: (ByteArrayFlow) -> ByteArrayFlow): OkioPlatformMedia {
        throw NotImplementedError("No use case was seen by the developer. Feel free to implement.")
    }

    override suspend fun getTemporaryFile(): Result<OkioPlatformMedia.TemporaryFile> {
        return Result.success(
            ReadOnlyTemporaryFile(path, fileSystem)
        )
    }

    suspend fun <T> map(transform: suspend (ByteArrayFlow) -> T): T {
        val byteArrayFlow = fileSystem.readByteArrayFlow(path)
        return if (byteArrayFlow != null) {
            transform(byteArrayFlow)
        } else {
            throw IOException("File does not exist: $path")
        }
    }

    override suspend fun toByteArray(
        coroutineScope: CoroutineScope?,
        expectedSize: Long?,
        maxSize: Long?
    ): ByteArray {
        return map { it.toByteArray() }
    }

    override suspend fun collect(collector: FlowCollector<ByteArray>) {
        map { it.collect(collector) }
    }

    class ReadOnlyTemporaryFile(override val path: Path, val fileSystem: FileSystem): OkioPlatformMedia.TemporaryFile {
        override suspend fun delete() {
            // read only
        }
    }
}
