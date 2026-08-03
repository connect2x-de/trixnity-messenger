package de.connect2x.trixnity.messenger.internal.adapters

import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.ViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.navigation.login
import de.connect2x.trixnity.messenger.internal.navigation.navigationCallback
import de.connect2x.trixnity.messenger.internal.routes.root.SSOLoginRoute
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.connecting.SSOLoginViewModel
import de.connect2x.trixnity.messenger.viewmodel.connecting.SSOLoginViewModelFactory
import org.koin.core.parameter.ParametersHolder

internal class SSOLoginViewModelFactoryAdapter(
    private val factory: SSOLoginViewModelFactory,
    private val routeNavigation: RouteNavigation,
) : ViewModelFactoryAdapter<SSOLoginViewModel> {
    override fun create(parameters: ParametersHolder): SSOLoginViewModel {
        val route = parameters.get<SSOLoginRoute>()
        val viewModelContext = parameters.get<ViewModelContext>().childContext("SSOLogin")

        val ssoLoginViewModel =
            factory.create(
                viewModelContext = viewModelContext,
                serverUrl = route.serverUrl,
                providerId = route.providerId,
                providerName = route.providerName,
                initialState = route.initialState,
                onLogin = routeNavigation.navigationCallback { login() },
                onBack = routeNavigation.navigationCallback { pop(route) },
            )

        if (route.redirectUri != null) {
            ssoLoginViewModel.resumeLogin(route.redirectUri)
        }

        return ssoLoginViewModel
    }
}
