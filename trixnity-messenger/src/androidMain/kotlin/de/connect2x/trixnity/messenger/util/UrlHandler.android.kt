package de.connect2x.trixnity.messenger.util

import android.net.Uri
import de.connect2x.trixnity.messenger.MatrixMessenger
import de.connect2x.trixnity.messenger.MatrixMessengerBaseConfiguration
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessenger
import org.koin.core.module.Module
import org.koin.dsl.module

class UriHandlerImpl(config: MatrixMessengerBaseConfiguration) : UriHandlerBase(config) {

    /** This need to be called by Activity Intent handler. */
    fun onUri(uri: Uri) {
        urlHandlerFlow.tryEmit(uri.toString())
    }
}

actual fun platformUriHandlerModule(): Module = module { single<UriHandler> { UriHandlerImpl(get()) } }

val MatrixMessenger.defaultUriHandler: UriHandlerImpl
    get() =
        checkNotNull(di.get<UriHandler>() as? UriHandlerImpl) {
            "default UrlHandler has been overridden and is not of expected type UrlHandlerImpl"
        }

val MatrixMultiMessenger.defaultUriHandler: UriHandlerImpl
    get() =
        checkNotNull(di.get<UriHandler>() as? UriHandlerImpl) {
            "default UrlHandler has been overridden and is not of expected type UrlHandlerImpl"
        }
