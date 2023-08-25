package de.connect2x.trixnity.messenger.util

import io.ktor.http.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

actual class UrlHandlerImpl(
    private val urlHandlerFlow: MutableSharedFlow<Url>
) : UrlHandler, Flow<Url> by urlHandlerFlow {

    actual constructor() : this(
        MutableSharedFlow(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
    )

    /**
     * This need to be called by application URL handler.
     */
    fun onUri(uri: String) {
        val url = Url(uri)
        urlHandlerFlow.tryEmit(url)
    }
}