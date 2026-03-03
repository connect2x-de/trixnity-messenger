package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.MatrixMessengerBaseConfiguration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filter
import org.koin.core.module.Module
import org.koin.dsl.module
import web.dom.document
import web.events.EventHandler
import web.events.EventType
import web.events.addEventListener
import web.events.removeEventListener
import web.window.window

class UriHandlerImpl private constructor(
    urlHandlerFlow: Flow<String>
) : UriHandler, Flow<String> by urlHandlerFlow {

    constructor(config: MatrixMessengerBaseConfiguration) : this(
        callbackFlow {
            val eventListener = EventHandler {
                trySend(document.URL)
            }
            window.addEventListener(EventType("locationchange"), eventListener)
            window.addEventListener(EventType("load"), eventListener)
            trySend(document.URL)
            awaitClose {
                window.removeEventListener(EventType("locationchange"), eventListener)
                window.removeEventListener(EventType("load"), eventListener)
            }
        }.filter(uriFilter(config))
    )
}

actual fun platformUriHandlerModule(): Module = module {
    single<UriHandler> {
        UriHandlerImpl(get())
    }
}
