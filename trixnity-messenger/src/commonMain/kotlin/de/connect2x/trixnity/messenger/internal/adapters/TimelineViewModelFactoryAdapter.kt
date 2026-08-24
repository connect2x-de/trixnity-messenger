package de.connect2x.trixnity.messenger.internal.adapters

import de.connect2x.trixnity.messenger.internal.logic.OpenMentionLogic
import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.ViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.navigation.closeRoom
import de.connect2x.trixnity.messenger.internal.navigation.navigationCallback
import de.connect2x.trixnity.messenger.internal.navigation.replace
import de.connect2x.trixnity.messenger.internal.routes.TimelineRoute
import de.connect2x.trixnity.messenger.internal.routes.extras.ExtrasRouteMarker
import de.connect2x.trixnity.messenger.internal.routes.extras.RoomSettingsRoute
import de.connect2x.trixnity.messenger.internal.routes.extras.TimelineElementMetadataRoute
import de.connect2x.trixnity.messenger.internal.routes.extras.UserProfileRoute
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineViewModelFactory
import kotlinx.coroutines.launch
import org.koin.core.parameter.ParametersHolder

internal class TimelineViewModelFactoryAdapter(
    private val factory: TimelineViewModelFactory,
    private val routeNavigation: RouteNavigation,
    private val openMentionLogic: OpenMentionLogic,
) : ViewModelFactoryAdapter<TimelineViewModel> {
    override fun create(parameters: ParametersHolder): TimelineViewModel {
        val route = parameters.get<TimelineRoute>()
        val viewModelContext = parameters.get<ViewModelContext>().childContext("Timeline", route.userId)

        return factory.create(
            viewModelContext = viewModelContext,
            roomId = route.roomId,
            onBack = routeNavigation.navigationCallback { closeRoom() },
            onOpenRoomSettings =
                routeNavigation.navigationCallback {
                    replace<ExtrasRouteMarker>(RoomSettingsRoute(route.userId, route.roomId))
                },
            onOpenUserProfile =
                routeNavigation.navigationCallback { theirUserId ->
                    push(UserProfileRoute(route.userId, route.roomId, theirUserId))
                },
            onOpenMention =
                routeNavigation.navigationCallback { userId, timelineElementMention ->
                    viewModelContext.coroutineScope.launch {
                        openMentionLogic.openMention(userId, timelineElementMention)
                    }
                },
            onOpenMetadata =
                routeNavigation.navigationCallback { eventId ->
                    push(TimelineElementMetadataRoute(route.userId, route.roomId, eventId))
                },
        )
    }
}
