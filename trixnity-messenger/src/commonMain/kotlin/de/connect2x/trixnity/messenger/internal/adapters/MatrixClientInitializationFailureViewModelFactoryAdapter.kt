package de.connect2x.trixnity.messenger.internal.adapters

import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.ViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.navigation.navigationCallback
import de.connect2x.trixnity.messenger.internal.routes.root.MatrixClientInitializationFailureRoute
import de.connect2x.trixnity.messenger.internal.routes.root.MatrixClientInitializationRoute
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.connecting.MatrixClientInitializationFailureViewModel
import de.connect2x.trixnity.messenger.viewmodel.connecting.MatrixClientInitializationFailureViewModelFactory
import org.koin.core.parameter.ParametersHolder

internal class MatrixClientInitializationFailureViewModelFactoryAdapter(
    private val factory: MatrixClientInitializationFailureViewModelFactory,
    private val routeNavigation: RouteNavigation,
) : ViewModelFactoryAdapter<MatrixClientInitializationFailureViewModel> {
    override fun create(parameters: ParametersHolder): MatrixClientInitializationFailureViewModel {
        val route = parameters.get<MatrixClientInitializationFailureRoute>()
        val viewModelContext = parameters.get<ViewModelContext>().childContext("MatrixClientInitializationFailure")

        return factory.create(
            viewModelContext = viewModelContext,
            userId = route.userId,
            initializationException = route.exception,
            onDeletionFinished = routeNavigation.navigationCallback { items = listOf(MatrixClientInitializationRoute) },
        )
    }
}
