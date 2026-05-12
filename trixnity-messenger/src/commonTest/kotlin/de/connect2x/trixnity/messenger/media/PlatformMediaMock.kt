package de.connect2x.trixnity.messenger.media

import de.connect2x.trixnity.client.media.PlatformMedia
import de.connect2x.trixnity.utils.ByteArrayFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.FlowCollector

object PlatformMediaMock : PlatformMedia {
    override fun transformByteArrayFlow(transformer: (ByteArrayFlow) -> ByteArrayFlow): PlatformMedia {
        throw NotImplementedError()
    }

    override suspend fun toByteArray(
        coroutineScope: CoroutineScope?,
        expectedSize: Long?,
        maxSize: Long?
    ): ByteArray? {
        throw NotImplementedError()
    }

    override suspend fun getTemporaryFile(): Result<PlatformMedia.TemporaryFile> {
        throw NotImplementedError()
    }

    override suspend fun collect(collector: FlowCollector<ByteArray>) {
        throw NotImplementedError()
    }
}
