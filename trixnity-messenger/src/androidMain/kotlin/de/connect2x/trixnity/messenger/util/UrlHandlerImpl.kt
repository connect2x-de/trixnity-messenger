package de.connect2x.trixnity.messenger.util

import android.net.Uri
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow

actual class UrlHandler actual constructor(filter: (Url) -> Boolean) : UrlHandlerBase(filter), Flow<Url> {

    /**
     * This need to be called by Activity Intent handler.
     */
    fun onUri(uri: Uri) {
        val url = Url(uri.toString())
        urlHandlerFlow.tryEmit(url)
    }
}