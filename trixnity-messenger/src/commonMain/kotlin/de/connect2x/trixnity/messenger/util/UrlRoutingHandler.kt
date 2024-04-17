package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.viewmodel.RootRouter.Config
import de.connect2x.trixnity.messenger.viewmodel.RootRouter.Wrapper
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope

interface UrlRoutingHandler {
    suspend fun onHandleUrl(
        coroutineScope: CoroutineScope,
        navigate: suspend (List<Config>) -> Wrapper,
        url: Url
    ): Boolean
}
