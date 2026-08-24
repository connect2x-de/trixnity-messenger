package de.connect2x.trixnity.messenger.internal.adapters

import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.ViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.navigation.navigationCallback
import de.connect2x.trixnity.messenger.internal.routes.extras.RoomDevInfoRoute
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomDevInfoViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomDevInfoViewModelFactory
import org.koin.core.parameter.ParametersHolder

internal class RoomDevInfoViewModelFactoryAdapter(
    private val factory: RoomDevInfoViewModelFactory,
    private val routeNavigation: RouteNavigation,
) : ViewModelFactoryAdapter<RoomDevInfoViewModel> {
    override fun create(parameters: ParametersHolder): RoomDevInfoViewModel {
        val route = parameters.get<RoomDevInfoRoute>()
        val viewModelContext = parameters.get<ViewModelContext>().childContext("RoomDevInfo", route.userId)

        return factory.create(
            viewModelContext = viewModelContext,
            roomId = route.roomId,
            onBack = routeNavigation.navigationCallback { pop(route) },
        )
    }
}
