package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.viewmodel.RootRouter.Config
import de.connect2x.trixnity.messenger.viewmodel.RootRouter.Wrapper
import io.ktor.http.*

interface UrlRoutingHandler {
    suspend fun onHandleUrl(url: Url, navigate: suspend (List<Config>) -> Wrapper): Boolean
}
