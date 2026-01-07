package de.connect2x.trixnity.messenger.compose.app

import de.connect2x.messenger.compose.view.startMultiMessenger
import io.ktor.http.*
import kotlinx.browser.window

suspend fun main() = startMultiMessenger(
    configuration = {
        configure()
        appUri = Url(window.location.toString()).protocolWithAuthority
        oAuth2ClientUrl = Url(window.location.toString()).protocolWithAuthority // dev only!
        messengerConfiguration {
            appUriSsoRedirect = "sso.html"
            appUriOAuth2Redirect = "oauth2.html"
        }
    }
)
