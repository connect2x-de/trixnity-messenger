package de.connect2x.trixnity.messenger.util

import io.ktor.http.*
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.w3c.dom.events.Event

actual class UrlHandlerImpl(
    private val urlHandlerFlow: Flow<Url>
) : UrlHandler, Flow<Url> by urlHandlerFlow {

    actual constructor() : this(
        callbackFlow {
            val eventListener: (Event) -> Unit = {
                trySend(Url(document.URL))
            }
            window.addEventListener("load", eventListener)
            awaitClose {
                window.removeEventListener("locationchange", eventListener)
            }
        }
    )
}