package de.connect2x.trixnity.messenger.internal.adapters

import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.ViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.navigation.navigationCallback
import de.connect2x.trixnity.messenger.internal.navigation.openRoom
import de.connect2x.trixnity.messenger.internal.navigation.replace
import de.connect2x.trixnity.messenger.internal.routes.roomlist.CreateNewChatRoute
import de.connect2x.trixnity.messenger.internal.routes.roomlist.CreateNewGroupRoute
import de.connect2x.trixnity.messenger.internal.routes.roomlist.RoomListRoute
import de.connect2x.trixnity.messenger.internal.routes.roomlist.RoomListRouteMarker
import de.connect2x.trixnity.messenger.internal.routes.roomlist.SearchGroupRoute
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewChatViewModel
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewChatViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewRoomViewModelFactory
import org.koin.core.parameter.ParametersHolder

internal class CreateNewChatViewModelFactoryAdapter(
    private val factory: CreateNewChatViewModelFactory,
    private val routeNavigation: RouteNavigation,
    private val createNewRoomViewModelFactory: CreateNewRoomViewModelFactory,
) : ViewModelFactoryAdapter<CreateNewChatViewModel> {
    override fun create(parameters: ParametersHolder): CreateNewChatViewModel {
        val route = parameters.get<CreateNewChatRoute>()
        val viewModelContext = parameters.get<ViewModelContext>().childContext("CreateNewChat", route.userId)
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
            onCreateGroup = routeNavigation.navigationCallback { userId -> push(CreateNewGroupRoute(userId)) },
            onSearchGroup = routeNavigation.navigationCallback { userId -> push(SearchGroupRoute(userId)) },
            onCancel = routeNavigation.navigationCallback { pop(route) },
        )
    }
}
