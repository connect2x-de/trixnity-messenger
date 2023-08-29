package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.viewmodel.settings.MessengerSettings
import io.ktor.http.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter

expect class UrlHandler(filter: (Url) -> Boolean) : Flow<Url>

open class UrlHandlerBase(
    filter: (Url) -> Boolean,
    protected val urlHandlerFlow: MutableSharedFlow<Url> =
        MutableSharedFlow(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
) : Flow<Url> by urlHandlerFlow.filter(filter)

fun createFilteringUrlHandler(messengerSettings: MessengerSettings): UrlHandler {
    return UrlHandler {
        it.protocol == URLProtocol.createOrDefault(messengerSettings.urlProtocol)
                && it.host == messengerSettings.urlHost
    }
}