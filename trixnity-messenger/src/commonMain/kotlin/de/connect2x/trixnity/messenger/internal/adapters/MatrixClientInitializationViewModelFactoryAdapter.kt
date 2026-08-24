package de.connect2x.trixnity.messenger.internal.adapters

import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.ViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.navigation.login
import de.connect2x.trixnity.messenger.internal.navigation.navigationCallback
import de.connect2x.trixnity.messenger.internal.navigation.replace
import de.connect2x.trixnity.messenger.internal.routes.root.AddMatrixAccountRoute
import de.connect2x.trixnity.messenger.internal.routes.root.MatrixClientInitializationFailureRoute
import de.connect2x.trixnity.messenger.internal.routes.root.RootRouteMarker
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.connecting.MatrixClientInitializationViewModel
import de.connect2x.trixnity.messenger.viewmodel.connecting.MatrixClientInitializationViewModelFactory
import org.koin.core.parameter.ParametersHolder

internal class MatrixClientInitializationViewModelFactoryAdapter(
    private val factory: MatrixClientInitializationViewModelFactory,
    private val routeNavigation: RouteNavigation,
) : ViewModelFactoryAdapter<MatrixClientInitializationViewModel> {

    override fun create(parameters: ParametersHolder): MatrixClientInitializationViewModel {
        val viewModelContext = parameters.get<ViewModelContext>().childContext("MatrixClientInitialization")

        return factory.create(
            viewModelContext = viewModelContext,
            onNoAccounts = routeNavigation.navigationCallback { push(AddMatrixAccountRoute) },
            onInitializationSuccess = routeNavigation.navigationCallback { login() },
            onInitializationFailure =
                routeNavigation.navigationCallback { userId, exception ->
                    replace<RootRouteMarker>(MatrixClientInitializationFailureRoute(userId, exception))
                },
        )
    }
}
