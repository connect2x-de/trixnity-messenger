package de.connect2x.trixnity.messenger.util

import io.ktor.http.*

class UrlHandlerImpl : UrlHandlerBase() {

    /**
     * This need to be called by application url handler.
     */
    fun onUri(uri: String) {
        val url = Url(uri)
        urlHandlerFlow.tryEmit(url)
    }
}