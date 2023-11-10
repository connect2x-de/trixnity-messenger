package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.viewmodel.settings.MessengerSettings
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow

class UrlHandlerImpl(messengerSettings: MessengerSettings) : UrlHandlerBase(messengerSettings), Flow<Url> {

    /**
     * This need to be called by application url handler.
     */
    fun onUri(uri: String) {
        val url = Url(uri)
        urlHandlerFlow.tryEmit(url)
    }
}