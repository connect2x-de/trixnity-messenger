package de.connect2x.trixnity.messenger.internal.workers

import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.Worker
import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.replace
import de.connect2x.trixnity.messenger.internal.routes.root.SSOLoginRoute
import de.connect2x.trixnity.messenger.util.UriHandler
import io.ktor.http.Url
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapNotNull

internal interface SSOLoginWorker : Worker

internal fun SSOLoginWorker(
    matrixMessengerConfiguration: MatrixMessengerConfiguration,
    matrixMessengerSettingsHolder: MatrixMessengerSettingsHolder,
    uriHandler: UriHandler,
    routeNavigation: RouteNavigation,
): SSOLoginWorker {
    return SSOLoginWorkerImpl(
        matrixMessengerConfiguration = matrixMessengerConfiguration,
        matrixMessengerSettingsHolder = matrixMessengerSettingsHolder,
        uriHandler = uriHandler,
        routeNavigation = routeNavigation,
    )
}

private class SSOLoginWorkerImpl(
    private val matrixMessengerConfiguration: MatrixMessengerConfiguration,
    private val matrixMessengerSettingsHolder: MatrixMessengerSettingsHolder,
    private val uriHandler: UriHandler,
    private val routeNavigation: RouteNavigation,
) : SSOLoginWorker {
    override suspend fun doWork() {
        combine(
                uriHandler.filter(::isSsoUri),
                matrixMessengerSettingsHolder.mapNotNull { it.base.ssoLoginState },
                ::makeRoute,
            )
            .collect(::updateNavigation)
    }

    private fun isSsoUri(uri: String): Boolean {
        return Url(uri).encodedPath == "/${matrixMessengerConfiguration.appUriSsoRedirect}"
    }

    private fun makeRoute(uri: String, state: MatrixMessengerSettingsBase.SSOLoginState): SSOLoginRoute {
        return SSOLoginRoute(
            serverUrl = state.serverUrl,
            providerId = state.providerId,
            providerName = state.providerName,
            initialState = state.state,
            redirectUri = uri,
        )
    }

    private fun updateNavigation(route: SSOLoginRoute) {
        routeNavigation.updateNavigation { replace<SSOLoginRoute>(route) }
    }
}
