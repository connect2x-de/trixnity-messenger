package de.connect2x.trixnity.messenger.internal.adapters

import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.ViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.navigation.login
import de.connect2x.trixnity.messenger.internal.navigation.navigationCallback
import de.connect2x.trixnity.messenger.internal.routes.root.RegisterMatrixAccountRoute
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.connecting.RegisterMatrixAccountViewModel
import de.connect2x.trixnity.messenger.viewmodel.connecting.RegisterMatrixAccountViewModelFactory
import org.koin.core.parameter.ParametersHolder

internal class RegisterMatrixAccountViewModelFactoryAdapter(
    private val factory: RegisterMatrixAccountViewModelFactory,
    private val routeNavigation: RouteNavigation,
) : ViewModelFactoryAdapter<RegisterMatrixAccountViewModel> {
    override fun create(parameters: ParametersHolder): RegisterMatrixAccountViewModel {
        val route = parameters.get<RegisterMatrixAccountRoute>()
        val viewModelContext = parameters.get<ViewModelContext>().childContext("RegisterMatrixAccount")

        return factory.create(
            viewModelContext = viewModelContext,
            serverUrl = route.serverUrl,
            onLogin = routeNavigation.navigationCallback { login() },
            onBack = routeNavigation.navigationCallback { pop(route) },
        )
    }
}
