package de.connect2x.trixnity.messenger.internal.adapters

import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.ViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.navigation.navigationCallback
import de.connect2x.trixnity.messenger.internal.navigation.openRoom
import de.connect2x.trixnity.messenger.internal.navigation.replace
import de.connect2x.trixnity.messenger.internal.routes.roomlist.RoomListRoute
import de.connect2x.trixnity.messenger.internal.routes.roomlist.RoomListRouteMarker
import de.connect2x.trixnity.messenger.internal.routes.roomlist.SearchGroupRoute
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.roomlist.SearchGroupViewModel
import de.connect2x.trixnity.messenger.viewmodel.roomlist.SearchGroupViewModelFactory
import org.koin.core.parameter.ParametersHolder

internal class SearchGroupViewModelFactoryAdapter(
    private val factory: SearchGroupViewModelFactory,
    private val routeNavigation: RouteNavigation,
) : ViewModelFactoryAdapter<SearchGroupViewModel> {
    override fun create(parameters: ParametersHolder): SearchGroupViewModel {
        val route = parameters.get<SearchGroupRoute>()
        val viewModelContext = parameters.get<ViewModelContext>().childContext("SearchGroup", route.userId)

        return factory.create(
            viewModelContext = viewModelContext,
            onBack = routeNavigation.navigationCallback { pop(route) },
            onGroupJoined =
                routeNavigation.navigationCallback { userId, roomId ->
                    replace<RoomListRouteMarker>(RoomListRoute)
                    openRoom(userId, roomId)
                },
            onGroupKnocked = routeNavigation.navigationCallback { _ -> replace<RoomListRouteMarker>(RoomListRoute) },
        )
    }
}
