package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import io.ktor.http.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import org.koin.core.module.Module

interface UrlHandler : Flow<Url>

open class UrlHandlerBase(
    config: MatrixMessengerConfiguration,
    filter: (Url) -> Boolean = urlFilter(config),
    protected val urlHandlerFlow: MutableSharedFlow<Url> =
        MutableSharedFlow(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
) : UrlHandler, Flow<Url> by urlHandlerFlow.filter(filter)

fun urlFilter(config: MatrixMessengerConfiguration): (Url) -> Boolean = {
    it.protocol == URLProtocol.createOrDefault(config.urlProtocol)
            && it.host == config.urlHost
}

expect fun platformUrlHandlerModule(): Module