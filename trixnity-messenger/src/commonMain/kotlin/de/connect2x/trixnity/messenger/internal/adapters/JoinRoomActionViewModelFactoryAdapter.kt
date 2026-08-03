package de.connect2x.trixnity.messenger.internal.adapters

import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.ViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.navigation.navigationCallback
import de.connect2x.trixnity.messenger.internal.navigation.openRoom
import de.connect2x.trixnity.messenger.internal.routes.JoinRoomActionRoute
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.JoinRoomActionViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.JoinRoomActionViewModelFactory
import org.koin.core.parameter.ParametersHolder

internal class JoinRoomActionViewModelFactoryAdapter(
    private val factory: JoinRoomActionViewModelFactory,
    private val routeNavigation: RouteNavigation,
) : ViewModelFactoryAdapter<JoinRoomActionViewModel> {
    override fun create(parameters: ParametersHolder): JoinRoomActionViewModel {
        val route = parameters.get<JoinRoomActionRoute>()
        val viewModelContext = parameters.get<ViewModelContext>().childContext("JoinRoomAction", route.userId)

        return factory.create(
            viewModelContext = viewModelContext,
            roomId = route.roomId,
            via = route.via,
            onOpenRoom = routeNavigation.navigationCallback { roomId -> openRoom(route.userId, roomId) },
            onDismiss = routeNavigation.navigationCallback { pop(route) },
        )
    }
}
