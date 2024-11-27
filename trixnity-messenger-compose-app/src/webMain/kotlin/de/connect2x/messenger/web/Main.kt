package de.connect2x.messenger.web

import de.connect2x.messenger.compose.view.startMessenger
import de.connect2x.messenger.messengerConfiguration
import kotlinx.browser.window

suspend fun main() = startMessenger(
    configuration = messengerConfiguration {
        urlProtocol = window.location.protocol.dropLast(1)
        urlHost = window.location.host
        messengerConfiguration {
            ssoRedirectPath = "sso.html"
        }
    }
)
