package de.connect2x.trixnity.messenger.util

import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import java.net.URI

actual class UrlHandler actual constructor(filter: (Url) -> Boolean) : UrlHandlerBase(filter), Flow<Url> {

    /**
     * This need to be called by Activity Intent handler.
     */
    fun onUri(uri: URI) {
        val url = Url(uri)
        urlHandlerFlow.tryEmit(url)
    }
}