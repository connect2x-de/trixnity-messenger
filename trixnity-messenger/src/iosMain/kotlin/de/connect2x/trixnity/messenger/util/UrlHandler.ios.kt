package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.MatrixMessengerBaseConfiguration
import io.ktor.http.*
import org.koin.core.module.Module
import org.koin.dsl.module

class UrlHandlerImpl(config: MatrixMessengerBaseConfiguration) : UrlHandlerBase(config) {

    /**
     * This need to be called by application url handler.
     */
    fun onUri(uri: String) {
        val url = Url(uri)
        urlHandlerFlow.tryEmit(url)
    }
}

actual fun platformUrlHandlerModule(): Module = module {
    single<UrlHandler> {
        UrlHandlerImpl(get())
    }
}
