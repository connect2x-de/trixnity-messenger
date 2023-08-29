package de.connect2x.trixnity.messenger.util

import io.ktor.http.*
import kotlinx.coroutines.flow.Flow

actual class UrlHandler actual constructor(filter: (Url) -> Boolean) : UrlHandlerBase(filter), Flow<Url> {

    /**
     * This need to be called by application URL handler.
     */
    fun onUri(uri: String) {
        val url = Url(uri)
        urlHandlerFlow.tryEmit(url)
    }
}