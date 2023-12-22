package de.connect2x.trixnity.messenger.util

import android.net.Uri
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import io.ktor.http.*
import net.folivo.trixnity.client.MatrixClient
import org.koin.core.module.Module
import org.koin.dsl.module

class UrlHandlerImpl(config: MatrixMessengerConfiguration) : UrlHandlerBase(config) {

    /**
     * This need to be called by Activity Intent handler.
     */
    fun onUri(uri: Uri) {
        val url = Url(uri.toString())
        urlHandlerFlow.tryEmit(url)
    }
}

actual fun platformUrlHandlerModule(): Module = module {
    single<UrlHandler> {
        UrlHandlerImpl(get())
    }
}

val MatrixClient.defaultUrlHandler: UrlHandlerImpl
    get() = checkNotNull(di.get<UrlHandler>() as? UrlHandlerImpl) {
        "default UrlHandler has been overridden and is not of expected type UrlHandlerImpl"
    }