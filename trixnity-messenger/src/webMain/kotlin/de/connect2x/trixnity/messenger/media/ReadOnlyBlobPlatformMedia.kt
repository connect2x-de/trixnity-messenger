package de.connect2x.trixnity.messenger.media

import de.connect2x.trixnity.client.media.indexeddb.IndexeddbPlatformMedia
import de.connect2x.trixnity.utils.ByteArrayFlow
import js.typedarrays.toByteArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.FlowCollector
import okio.IOException
import web.blob.Blob
import web.blob.bytes

class ReadOnlyBlobPlatformMedia(val blob: Blob) : IndexeddbPlatformMedia {
    override fun transformByteArrayFlow(transformer: (ByteArrayFlow) -> ByteArrayFlow): IndexeddbPlatformMedia {
        throw NotImplementedError("No use case was seen by the developer. Feel free to implement.")
    }

    suspend fun <T> map(transform: suspend (ByteArray) -> T): T {
        val byteArrayFlow = blob?.bytes()?.toByteArray()
        return if (byteArrayFlow != null) {
            transform(byteArrayFlow)
        } else {
            throw IOException()
        }
    }

    override suspend fun toByteArray(
        coroutineScope: CoroutineScope?,
        expectedSize: Long?,
        maxSize: Long?,
    ): ByteArray {
        return map { it }
    }

    override suspend fun collect(collector: FlowCollector<ByteArray>) {
        map { collector.emit(it) }
    }

    override suspend fun getTemporaryFile(): Result<ReadOnlyTemporaryFile> {
        return Result.success(ReadOnlyTemporaryFile(blob))
    }

    class ReadOnlyTemporaryFile(override val file: Blob) : IndexeddbPlatformMedia.TemporaryFile {
        override suspend fun delete() {
            // read only
        }
    }
}
