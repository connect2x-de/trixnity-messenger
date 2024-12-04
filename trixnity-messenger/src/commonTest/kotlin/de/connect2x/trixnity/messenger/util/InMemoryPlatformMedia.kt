package de.connect2x.trixnity.messenger.util

import net.folivo.trixnity.client.media.PlatformMedia
import net.folivo.trixnity.utils.ByteArrayFlow
import net.folivo.trixnity.utils.toByteArrayFlow

class InMemoryPlatformMedia(private val delegate: ByteArrayFlow) : PlatformMedia,
    ByteArrayFlow by delegate {
    constructor(byteArray: ByteArray) : this(byteArray.toByteArrayFlow())

    override fun transformByteArrayFlow(transformer: (ByteArrayFlow) -> ByteArrayFlow): PlatformMedia =
        InMemoryPlatformMedia(delegate.let(transformer))
}
