package de.connect2x.trixnity.messenger.internal.adapters

import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.ViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.navigation.navigationCallback
import de.connect2x.trixnity.messenger.internal.routes.extras.PowerlevelRoute
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.settings.PowerlevelViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.settings.PowerlevelViewModelFactory
import org.koin.core.parameter.ParametersHolder

internal class PowerlevelViewModelFactoryAdapter(
    private val factory: PowerlevelViewModelFactory,
    private val routeNavigation: RouteNavigation,
) : ViewModelFactoryAdapter<PowerlevelViewModel> {
    override fun create(parameters: ParametersHolder): PowerlevelViewModel {
        val route = parameters.get<PowerlevelRoute>()
        val viewModelContext = parameters.get<ViewModelContext>().childContext("Powerlevel", route.userId)

        return factory.create(
            viewModelContext = viewModelContext,
            roomId = route.roomId,
            onBack = routeNavigation.navigationCallback { pop(route) },
        )
    }
}
