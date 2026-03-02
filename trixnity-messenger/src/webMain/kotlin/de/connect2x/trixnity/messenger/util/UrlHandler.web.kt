package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.MatrixMessengerBaseConfiguration
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filter
import org.koin.core.module.Module
import org.koin.dsl.module
import org.w3c.dom.events.Event

class UriHandlerImpl private constructor(
    urlHandlerFlow: Flow<String>
) : UriHandler, Flow<String> by urlHandlerFlow {

    constructor(config: MatrixMessengerBaseConfiguration) : this(
        callbackFlow {
            val eventListener: (Event?) -> Unit = {
                trySend(document.URL)
            }
            window.addEventListener("locationchange", eventListener)
            window.addEventListener("load", eventListener)
            eventListener(null)
            awaitClose {
                window.removeEventListener("locationchange", eventListener)
                window.removeEventListener("load", eventListener)
            }
        }.filter(uriFilter(config))
    )
}

actual fun platformUriHandlerModule(): Module = module {
    single<UriHandler> {
        UriHandlerImpl(get())
    }
}
