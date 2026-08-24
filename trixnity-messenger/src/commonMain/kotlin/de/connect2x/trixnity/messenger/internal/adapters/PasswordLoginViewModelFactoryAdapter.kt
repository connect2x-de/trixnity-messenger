package de.connect2x.trixnity.messenger.internal.adapters

import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.ViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.navigation.login
import de.connect2x.trixnity.messenger.internal.navigation.navigationCallback
import de.connect2x.trixnity.messenger.internal.routes.root.PasswordLoginRoute
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.connecting.PasswordLoginViewModel
import de.connect2x.trixnity.messenger.viewmodel.connecting.PasswordLoginViewModelFactory
import org.koin.core.parameter.ParametersHolder

internal class PasswordLoginViewModelFactoryAdapter(
    private val factory: PasswordLoginViewModelFactory,
    private val routeNavigation: RouteNavigation,
) : ViewModelFactoryAdapter<PasswordLoginViewModel> {
    override fun create(parameters: ParametersHolder): PasswordLoginViewModel {
        val route = parameters.get<PasswordLoginRoute>()
        val viewModelContext = parameters.get<ViewModelContext>().childContext("PasswordLogin")

        return factory.create(
            viewModelContext = viewModelContext,
            serverUrl = route.serverUrl,
            onLogin = routeNavigation.navigationCallback { login() },
            onBack = routeNavigation.navigationCallback { pop(route) },
        )
    }
}
