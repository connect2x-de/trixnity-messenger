package de.connect2x.trixnity.messenger.internal.adapters

import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.ViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.navigation.login
import de.connect2x.trixnity.messenger.internal.navigation.navigationCallback
import de.connect2x.trixnity.messenger.internal.routes.root.OAuth2AuthorizationCodeLoginRoute
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.connecting.OAuth2AuthorizationCodeLoginViewModel
import de.connect2x.trixnity.messenger.viewmodel.connecting.OAuth2AuthorizationCodeLoginViewModelFactory
import org.koin.core.parameter.ParametersHolder

internal class OAuth2AuthorizationCodeLoginViewModelFactoryAdapter(
    private val factory: OAuth2AuthorizationCodeLoginViewModelFactory,
    private val routeNavigation: RouteNavigation,
) : ViewModelFactoryAdapter<OAuth2AuthorizationCodeLoginViewModel> {
    override fun create(parameters: ParametersHolder): OAuth2AuthorizationCodeLoginViewModel {
        val route = parameters.get<OAuth2AuthorizationCodeLoginRoute>()
        val viewModelContext = parameters.get<ViewModelContext>().childContext("OAuth2AuthorizationCodeLogin")

        val oAuth2AuthorizationCodeLoginViewModel =
            factory.create(
                viewModelContext = viewModelContext,
                type = route.kind,
                serverUrl = route.serverUrl,
                initialState = route.initialState,
                onLogin = routeNavigation.navigationCallback { login() },
                onBack = routeNavigation.navigationCallback { pop(route) },
            )

        if (route.redirectUri != null) {
            oAuth2AuthorizationCodeLoginViewModel.resumeLogin(route.redirectUri)
        }

        return oAuth2AuthorizationCodeLoginViewModel
    }
}
