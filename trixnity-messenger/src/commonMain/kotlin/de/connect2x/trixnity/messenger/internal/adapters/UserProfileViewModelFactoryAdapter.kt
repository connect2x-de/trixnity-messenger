package de.connect2x.trixnity.messenger.internal.adapters

import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.ViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.navigation.clear
import de.connect2x.trixnity.messenger.internal.navigation.navigationCallback
import de.connect2x.trixnity.messenger.internal.navigation.replace
import de.connect2x.trixnity.messenger.internal.routes.TimelineRoute
import de.connect2x.trixnity.messenger.internal.routes.extras.ExtrasRouteMarker
import de.connect2x.trixnity.messenger.internal.routes.extras.UserProfileRoute
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.settings.UserProfileViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.settings.UserProfileViewModelFactory
import org.koin.core.parameter.ParametersHolder

internal class UserProfileViewModelFactoryAdapter(
    private val factory: UserProfileViewModelFactory,
    private val routeNavigation: RouteNavigation,
) : ViewModelFactoryAdapter<UserProfileViewModel> {
    override fun create(parameters: ParametersHolder): UserProfileViewModel {
        val route = parameters.get<UserProfileRoute>()
        val viewModelContext = parameters.get<ViewModelContext>().childContext("UserProfile", route.userId)

        return factory.create(
            viewModelContext = viewModelContext,
            userId = route.theirUserId,
            selectedRoomId = route.roomId,
            onOpenRoom =
                routeNavigation.navigationCallback { userId, roomId ->
                    clear<ExtrasRouteMarker>()
                    replace<TimelineRoute>(TimelineRoute(userId, roomId))
                },
            onBack = routeNavigation.navigationCallback { pop(route) },
            onCloseSettings = routeNavigation.navigationCallback { clear<ExtrasRouteMarker>() },
        )
    }
}
