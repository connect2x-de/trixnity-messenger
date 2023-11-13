package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.viewmodel.settings.MessengerSettings
import io.ktor.http.*

class UrlHandlerImpl(messengerSettings: MessengerSettings) : UrlHandlerBase(messengerSettings), UrlHandler {

    /**
     * This need to be called by application url handler.
     */
    fun onUri(uri: String) {
        val url = Url(uri)
        urlHandlerFlow.tryEmit(url)
    }
}