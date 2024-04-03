package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.MatrixMessengerBaseConfiguration
import io.ktor.http.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import org.koin.core.module.Module

interface UrlHandler : Flow<Url> // there is no multiplatform Uri that we are aware of, so we use Url

open class UrlHandlerBase(
    config: MatrixMessengerBaseConfiguration,
    filter: (Url) -> Boolean = urlFilter(config),
    protected val urlHandlerFlow: MutableSharedFlow<Url> =
        MutableSharedFlow(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
) : UrlHandler, Flow<Url> by urlHandlerFlow.filter(filter)

fun urlFilter(config: MatrixMessengerBaseConfiguration): (Url) -> Boolean = {
    it.protocol == URLProtocol.createOrDefault(config.urlProtocol)
            && it.host == config.urlHost
}

expect fun platformUrlHandlerModule(): Module
