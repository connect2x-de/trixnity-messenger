package de.connect2x.trixnity.messenger.util

import android.net.Uri
import io.ktor.http.*

class UrlHandlerImpl : UrlHandlerBase() {

    /**
     * This need to be called by Activity Intent handler.
     */
    fun onUri(uri: Uri) {
        val url = Url(uri.toString())
        urlHandlerFlow.tryEmit(url)
    }
}