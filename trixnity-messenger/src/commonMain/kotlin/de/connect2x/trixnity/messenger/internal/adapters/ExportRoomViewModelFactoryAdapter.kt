package de.connect2x.trixnity.messenger.internal.adapters

import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.ViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.navigation.navigationCallback
import de.connect2x.trixnity.messenger.internal.routes.extras.ExportRoomRoute
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExportRoomViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExportRoomViewModelFactory
import org.koin.core.parameter.ParametersHolder

internal class ExportRoomViewModelFactoryAdapter(
    private val factory: ExportRoomViewModelFactory,
    private val routeNavigation: RouteNavigation,
) : ViewModelFactoryAdapter<ExportRoomViewModel> {
    override fun create(parameters: ParametersHolder): ExportRoomViewModel {
        val route = parameters.get<ExportRoomRoute>()
        val viewModelContext = parameters.get<ViewModelContext>().childContext("ExportRoom", route.userId)

        return factory.create(
            viewModelContext = viewModelContext,
            roomId = route.roomId,
            onBack = routeNavigation.navigationCallback { pop(route) },
        )
    }
}
