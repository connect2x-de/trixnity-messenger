package de.connect2x.trixnity.messenger.internal.workers

import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.Worker
import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.replace
import de.connect2x.trixnity.messenger.internal.routes.root.OAuth2AuthorizationCodeLoginRoute
import de.connect2x.trixnity.messenger.util.UriHandler
import io.ktor.http.Url
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapNotNull

internal interface OAuth2AuthorizationCodeLoginWorker : Worker

internal fun OAuth2AuthorizationCodeLoginWorker(
    matrixMessengerConfiguration: MatrixMessengerConfiguration,
    matrixMessengerSettingsHolder: MatrixMessengerSettingsHolder,
    uriHandler: UriHandler,
    routeNavigation: RouteNavigation,
): OAuth2AuthorizationCodeLoginWorker {
    return OAuth2AuthorizationCodeLoginWorkerImpl(
        matrixMessengerConfiguration = matrixMessengerConfiguration,
        matrixMessengerSettingsHolder = matrixMessengerSettingsHolder,
        uriHandler = uriHandler,
        routeNavigation = routeNavigation,
    )
}

private class OAuth2AuthorizationCodeLoginWorkerImpl(
    private val matrixMessengerConfiguration: MatrixMessengerConfiguration,
    private val matrixMessengerSettingsHolder: MatrixMessengerSettingsHolder,
    private val uriHandler: UriHandler,
    private val routeNavigation: RouteNavigation,
) : OAuth2AuthorizationCodeLoginWorker {
    override suspend fun doWork() {
        combine(
                uriHandler.filter(::isOauth2Uri),
                matrixMessengerSettingsHolder.mapNotNull { it.base.oAuth2LoginState },
                ::makeRoute,
            )
            .collect(::updateNavigation)
    }

    private fun isOauth2Uri(uri: String): Boolean {
        return Url(uri).encodedPath == "/${matrixMessengerConfiguration.appUriOAuth2Redirect}"
    }

    private fun makeRoute(
        uri: String,
        state: MatrixMessengerSettingsBase.OAuth2LoginState,
    ): OAuth2AuthorizationCodeLoginRoute {
        return OAuth2AuthorizationCodeLoginRoute(
            serverUrl = state.serverUrl,
            kind = state.type,
            initialState = state.state,
            redirectUri = uri,
        )
    }

    private fun updateNavigation(route: OAuth2AuthorizationCodeLoginRoute) {
        routeNavigation.updateNavigation { replace<OAuth2AuthorizationCodeLoginRoute>(route) }
    }
}
