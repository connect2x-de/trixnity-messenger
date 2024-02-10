package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.MatrixMessenger
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessenger
import io.ktor.http.*
import org.koin.core.module.Module
import org.koin.dsl.module

class UrlHandlerImpl(config: MatrixMessengerConfiguration) : UrlHandlerBase(config) {

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

val MatrixMessenger.defaultUrlHandler: UrlHandlerImpl
    get() = checkNotNull(di.get<UrlHandler>() as? UrlHandlerImpl) {
        "default UrlHandler has been overridden and is not of expected type UrlHandlerImpl"
    }

val MatrixMultiMessenger.defaultUrlHandler: UrlHandlerImpl
    get() = checkNotNull(di.get<UrlHandler>() as? UrlHandlerImpl) {
        "default UrlHandler has been overridden and is not of expected type UrlHandlerImpl"
    }