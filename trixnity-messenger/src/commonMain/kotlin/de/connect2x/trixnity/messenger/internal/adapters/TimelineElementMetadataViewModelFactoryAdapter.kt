package de.connect2x.trixnity.messenger.internal.adapters

import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.ViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.navigation.navigationCallback
import de.connect2x.trixnity.messenger.internal.routes.extras.TimelineElementDevInfoRoute
import de.connect2x.trixnity.messenger.internal.routes.extras.TimelineElementMetadataRoute
import de.connect2x.trixnity.messenger.internal.routes.extras.UserProfileRoute
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.settings.TimelineElementMetadataViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.settings.TimelineElementMetadataViewModelFactory
import org.koin.core.parameter.ParametersHolder

internal class TimelineElementMetadataViewModelFactoryAdapter(
    private val factory: TimelineElementMetadataViewModelFactory,
    private val routeNavigation: RouteNavigation,
) : ViewModelFactoryAdapter<TimelineElementMetadataViewModel> {
    override fun create(parameters: ParametersHolder): TimelineElementMetadataViewModel {
        val route = parameters.get<TimelineElementMetadataRoute>()
        val viewModelContext = parameters.get<ViewModelContext>().childContext("TimelineElementMetadata", route.userId)

        return factory.create(
            viewModelContext = viewModelContext,
            eventId = route.eventId,
            roomId = route.roomId,
            onOpenUserProfile =
                routeNavigation.navigationCallback { theirUserId ->
                    push(UserProfileRoute(userId = route.userId, roomId = route.roomId, theirUserId = theirUserId))
                },
            onOpenDevInfo =
                routeNavigation.navigationCallback {
                    push(
                        TimelineElementDevInfoRoute(
                            userId = route.userId,
                            roomId = route.roomId,
                            eventId = route.eventId,
                        )
                    )
                },
            onBack = routeNavigation.navigationCallback { pop(route) },
        )
    }
}
