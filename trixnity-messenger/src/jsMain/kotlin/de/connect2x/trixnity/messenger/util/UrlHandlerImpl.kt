package de.connect2x.trixnity.messenger.util

import io.ktor.http.*
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filter
import org.w3c.dom.events.Event

actual class UrlHandler(
    private val urlHandlerFlow: Flow<Url>
) : Flow<Url> by urlHandlerFlow {

    actual constructor(filter: (Url) -> Boolean) : this(
        callbackFlow {
            val eventListener: (Event) -> Unit = {
                trySend(Url(document.URL))
            }
            window.addEventListener("load", eventListener)
            awaitClose {
                window.removeEventListener("locationchange", eventListener)
            }
        }.filter(filter)
    )
}