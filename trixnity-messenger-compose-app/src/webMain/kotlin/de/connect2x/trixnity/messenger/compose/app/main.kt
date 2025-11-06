package de.connect2x.trixnity.messenger.compose.app

import de.connect2x.messenger.compose.view.startMultiMessenger
import kotlinx.browser.window

suspend fun main() = startMultiMessenger(
    configuration = {
        configure()
        urlProtocol = window.location.protocol.dropLast(1)
        urlHost = window.location.host
        messengerConfiguration {
            ssoRedirectPath = "sso.html"
        }
    }
)
