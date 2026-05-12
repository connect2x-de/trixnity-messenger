package de.connect2x.trixnity.messenger.util

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.lognity.api.logger.warn
import de.connect2x.trixnity.messenger.MatrixMessengerBaseConfiguration
import io.ktor.http.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onEach
import org.koin.core.module.Module

private val log: Logger = Logger("de.connect2x.trixnity.messenger.util.UriHandlerKt")

/**
 * There is no common Uri type in Kotlin Multiplatform. Therefore [String] is used.
 */
interface UriHandler : Flow<String>

open class UriHandlerBase(
    config: MatrixMessengerBaseConfiguration,
    filter: (String) -> Boolean = uriFilter(config),
    protected val urlHandlerFlow: MutableSharedFlow<String> =
        MutableSharedFlow(
            replay = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
) : UriHandler, Flow<String> by urlHandlerFlow.filter(filter).onEach({ log.info { "handle uri: $it" } })

fun uriFilter(config: MatrixMessengerBaseConfiguration): (String) -> Boolean = {
    try {
        Url(it).protocolWithAuthority == Url(config.appUri).protocolWithAuthority
    } catch (e: URLParserException) {
        log.warn(e) { "cannot parse URL" }
        false
    }
}

expect fun platformUriHandlerModule(): Module
