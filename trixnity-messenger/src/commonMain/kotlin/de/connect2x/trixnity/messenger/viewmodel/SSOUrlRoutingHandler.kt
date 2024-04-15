package de.connect2x.trixnity.messenger.viewmodel

import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.util.UrlRoutingHandler
import io.ktor.http.*

class SSOUrlRoutingHandler(
    val config: MatrixMessengerConfiguration
) : UrlRoutingHandler {
    override fun onHandleUrl(router: RootRouter, url: Url): Boolean {
        if (url.encodedPath == config.ssoRedirectPath) {
            router.showSSOLogin {
                it.viewModel.resumeLogin(url)
            }
            return true
        }
        return false
    }
}
