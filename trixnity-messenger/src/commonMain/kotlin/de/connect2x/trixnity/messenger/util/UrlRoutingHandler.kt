package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.viewmodel.RootRouter
import io.ktor.http.*

interface UrlRoutingHandler {
    fun onHandleUrl(router: RootRouter, url: Url): Boolean
}
