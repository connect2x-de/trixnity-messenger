package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.viewmodel.settings.MessengerSettings
import io.ktor.http.*
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filter
import org.w3c.dom.events.Event

class UrlHandlerImpl private constructor(
    urlHandlerFlow: Flow<Url>
) : Flow<Url> by urlHandlerFlow {

    constructor(messengerSettings: MessengerSettings) : this(
        callbackFlow {
            val eventListener: (Event) -> Unit = {
                trySend(Url(document.URL))
            }
            window.addEventListener("load", eventListener)
            awaitClose {
                window.removeEventListener("locationchange", eventListener)
            }
        }.filter(urlFilter(messengerSettings))
    )
}