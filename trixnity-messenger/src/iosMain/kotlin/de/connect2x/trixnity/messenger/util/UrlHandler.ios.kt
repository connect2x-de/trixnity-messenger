package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.MatrixMessengerBaseConfiguration
import org.koin.core.module.Module
import org.koin.dsl.module

class UriHandlerImpl(config: MatrixMessengerBaseConfiguration) : UriHandlerBase(config) {

    /**
     * This need to be called by application url handler.
     */
    fun onUri(uri: String) {
        urlHandlerFlow.tryEmit(uri)
    }
}

actual fun platformUriHandlerModule(): Module = module {
    single<UriHandler> {
        UriHandlerImpl(get())
    }
}
