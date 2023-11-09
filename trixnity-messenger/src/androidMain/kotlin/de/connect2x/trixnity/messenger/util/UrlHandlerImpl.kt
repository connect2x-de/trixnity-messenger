package de.connect2x.trixnity.messenger.util

import android.net.Uri
import de.connect2x.trixnity.messenger.viewmodel.settings.MessengerSettings
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow

class UrlHandlerImpl(messengerSettings: MessengerSettings) : UrlHandlerBase(messengerSettings), Flow<Url> {

    /**
     * This need to be called by Activity Intent handler.
     */
    fun onUri(uri: Uri) {
        val url = Url(uri.toString())
        urlHandlerFlow.tryEmit(url)
    }
}