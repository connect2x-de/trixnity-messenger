package de.connect2x.trixnity.messenger.internal.adapters

import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.ViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.navigation.navigationCallback
import de.connect2x.trixnity.messenger.internal.navigation.openRoom
import de.connect2x.trixnity.messenger.internal.navigation.replace
import de.connect2x.trixnity.messenger.internal.routes.roomlist.CreateNewGroupRoute
import de.connect2x.trixnity.messenger.internal.routes.roomlist.RoomListRoute
import de.connect2x.trixnity.messenger.internal.routes.roomlist.RoomListRouteMarker
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewGroupViewModel
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewGroupViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewRoomViewModelFactory
import org.koin.core.parameter.ParametersHolder

internal class CreateNewGroupViewModelFactoryAdapter(
    private val factory: CreateNewGroupViewModelFactory,
    private val routeNavigation: RouteNavigation,
    private val createNewRoomViewModelFactory: CreateNewRoomViewModelFactory,
) : ViewModelFactoryAdapter<CreateNewGroupViewModel> {
    override fun create(parameters: ParametersHolder): CreateNewGroupViewModel {
        val route = parameters.get<CreateNewGroupRoute>()
        val viewModelContext = parameters.get<ViewModelContext>().childContext("CreateNewGroup", route.userId)
        val createNewRoomViewModelContext = viewModelContext.childContext("CreateNewRoom")

        return factory.create(
            viewModelContext = viewModelContext,
            createNewRoomViewModel =
                createNewRoomViewModelFactory.create(
                    viewModelContext = createNewRoomViewModelContext,
                    onRoomCreated =
                        routeNavigation.navigationCallback { userId, roomId ->
                            replace<RoomListRouteMarker>(RoomListRoute)
                            openRoom(userId, roomId)
                        },
                ),
            onBack = routeNavigation.navigationCallback { pop(route) },
        )
    }
}
