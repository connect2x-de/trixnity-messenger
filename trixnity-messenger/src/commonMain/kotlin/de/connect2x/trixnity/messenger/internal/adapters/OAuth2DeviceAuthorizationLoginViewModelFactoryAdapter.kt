package de.connect2x.trixnity.messenger.internal.adapters

import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.ViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.navigation.login
import de.connect2x.trixnity.messenger.internal.navigation.navigationCallback
import de.connect2x.trixnity.messenger.internal.routes.root.OAuth2DeviceAuthorizationLoginRoute
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.connecting.OAuth2DeviceAuthorizationLoginViewModel
import de.connect2x.trixnity.messenger.viewmodel.connecting.OAuth2DeviceAuthorizationLoginViewModelFactory
import org.koin.core.parameter.ParametersHolder

internal class OAuth2DeviceAuthorizationLoginViewModelFactoryAdapter(
    private val factory: OAuth2DeviceAuthorizationLoginViewModelFactory,
    private val routeNavigation: RouteNavigation,
) : ViewModelFactoryAdapter<OAuth2DeviceAuthorizationLoginViewModel> {
    override fun create(parameters: ParametersHolder): OAuth2DeviceAuthorizationLoginViewModel {
        val route = parameters.get<OAuth2DeviceAuthorizationLoginRoute>()
        val viewModelContext = parameters.get<ViewModelContext>().childContext("OAuth2DeviceAuthorizationLogin")

        return factory.create(
            viewModelContext = viewModelContext,
            serverUrl = route.serverUrl,
            onLogin = routeNavigation.navigationCallback { login() },
            onBack = routeNavigation.navigationCallback { pop(route) },
        )
    }
}
