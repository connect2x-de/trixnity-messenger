package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.MatrixMessengerBaseConfiguration
import io.ktor.http.*
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filter
import org.koin.core.module.Module
import org.koin.dsl.module
import org.w3c.dom.events.Event

class UrlHandlerImpl private constructor(
    urlHandlerFlow: Flow<Url>
) : UrlHandler, Flow<Url> by urlHandlerFlow {

    constructor(config: MatrixMessengerBaseConfiguration) : this(
        callbackFlow {
            val eventListener: (Event?) -> Unit = {
                trySend(Url(document.URL))
            }
            window.addEventListener("locationchange", eventListener)
            window.addEventListener("load", eventListener)
            eventListener(null)
            awaitClose {
                window.removeEventListener("locationchange", eventListener)
                window.removeEventListener("load", eventListener)
            }
        }.filter(urlFilter(config))
    )
}

actual fun platformUrlHandlerModule(): Module = module {
    single<UrlHandler> {
        UrlHandlerImpl(get())
    }
}
