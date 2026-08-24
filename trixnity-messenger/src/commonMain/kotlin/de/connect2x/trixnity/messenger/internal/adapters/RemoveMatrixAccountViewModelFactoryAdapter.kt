package de.connect2x.trixnity.messenger.internal.adapters

import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.ViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.navigation.navigationCallback
import de.connect2x.trixnity.messenger.internal.routes.root.MatrixClientInitializationRoute
import de.connect2x.trixnity.messenger.internal.routes.root.RemoveMatrixAccountRoute
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.connecting.RemoveMatrixAccountViewModel
import de.connect2x.trixnity.messenger.viewmodel.connecting.RemoveMatrixAccountViewModelFactory
import org.koin.core.parameter.ParametersHolder

internal class RemoveMatrixAccountViewModelFactoryAdapter(
    private val factory: RemoveMatrixAccountViewModelFactory,
    private val routeNavigation: RouteNavigation,
    private val matrixMessengerSettingsHolder: MatrixMessengerSettingsHolder,
) : ViewModelFactoryAdapter<RemoveMatrixAccountViewModel> {
    override fun create(parameters: ParametersHolder): RemoveMatrixAccountViewModel {
        val route = parameters.get<RemoveMatrixAccountRoute>()
        val viewModelContext = parameters.get<ViewModelContext>().childContext("RemoveMatrixAccount")

        return factory.create(
            viewModelContext = viewModelContext,
            userId = route.userId,
            onRemoveCompleted =
                routeNavigation.navigationCallback {
                    if (matrixMessengerSettingsHolder.value.base.accounts.isEmpty()) {
                        viewModelContext.log.debug { "since all account have been removed, close all navigation" }
                        items = listOf(MatrixClientInitializationRoute)
                        viewModelContext.log.debug { "finished closing all navigation" }
                    } else {
                        pop(route)
                    }
                },
        )
    }
}
