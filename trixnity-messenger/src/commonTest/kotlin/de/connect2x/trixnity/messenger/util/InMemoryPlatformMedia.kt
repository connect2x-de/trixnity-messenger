package de.connect2x.trixnity.messenger.util

import kotlinx.coroutines.CoroutineScope
import de.connect2x.trixnity.client.media.PlatformMedia
import de.connect2x.trixnity.utils.ByteArrayFlow
import de.connect2x.trixnity.utils.toByteArray
import de.connect2x.trixnity.utils.toByteArrayFlow

class InMemoryPlatformMedia(private val delegate: ByteArrayFlow) : PlatformMedia,
    ByteArrayFlow by delegate {
    constructor(byteArray: ByteArray) : this(byteArray.toByteArrayFlow())

    override fun transformByteArrayFlow(transformer: (ByteArrayFlow) -> ByteArrayFlow): PlatformMedia =
        InMemoryPlatformMedia(delegate.let(transformer))

    override suspend fun toByteArray(
        coroutineScope: CoroutineScope?,
        expectedSize: Long?,
        maxSize: Long?
    ): ByteArray = delegate.toByteArray()
}
