package de.connect2x.trixnity.messenger.internal.adapters

import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.messenger.internal.logic.OpenMentionLogic
import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.ViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.navigation.clear
import de.connect2x.trixnity.messenger.internal.navigation.closeRoom
import de.connect2x.trixnity.messenger.internal.navigation.navigationCallback
import de.connect2x.trixnity.messenger.internal.navigation.replace
import de.connect2x.trixnity.messenger.internal.routes.AvatarCutterRoute
import de.connect2x.trixnity.messenger.internal.routes.extras.AddMembersRoute
import de.connect2x.trixnity.messenger.internal.routes.extras.ExportRoomRoute
import de.connect2x.trixnity.messenger.internal.routes.extras.ExtrasRouteMarker
import de.connect2x.trixnity.messenger.internal.routes.extras.PowerlevelRoute
import de.connect2x.trixnity.messenger.internal.routes.extras.RoomDevInfoRoute
import de.connect2x.trixnity.messenger.internal.routes.extras.RoomSettingsRoute
import de.connect2x.trixnity.messenger.internal.routes.extras.UserProfileRoute
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsViewModelFactory
import kotlinx.coroutines.launch
import org.koin.core.parameter.ParametersHolder

internal class RoomSettingsViewModelFactoryAdapter(
    private val factory: RoomSettingsViewModelFactory,
    private val routeNavigation: RouteNavigation,
    private val openMentionLogic: OpenMentionLogic,
) : ViewModelFactoryAdapter<RoomSettingsViewModel> {
    override fun create(parameters: ParametersHolder): RoomSettingsViewModel {
        val route = parameters.get<RoomSettingsRoute>()
        val viewModelContext = parameters.get<ViewModelContext>().childContext("RoomSettings", route.userId)

        return factory.createWithNavigation(viewModelContext, route.roomId, routeNavigation, openMentionLogic)
    }
}

internal fun RoomSettingsViewModelFactory.createWithNavigation(
    viewModelContext: MatrixClientViewModelContext,
    roomId: RoomId,
    routeNavigation: RouteNavigation,
    openMentionLogic: OpenMentionLogic,
): RoomSettingsViewModel {
    return create(
        viewModelContext = viewModelContext,
        selectedRoomId = roomId,
        onCloseRoom = routeNavigation.navigationCallback { closeRoom() },
        onOpenAddMembers =
            routeNavigation.navigationCallback { push(AddMembersRoute(viewModelContext.userId, roomId)) },
        onOpenDevInfo = routeNavigation.navigationCallback { push(RoomDevInfoRoute(viewModelContext.userId, roomId)) },
        onOpenExportRoom =
            routeNavigation.navigationCallback { push(ExportRoomRoute(viewModelContext.userId, roomId)) },
        onCloseRoomSettings = routeNavigation.navigationCallback { clear<ExtrasRouteMarker>() },
        onOpenUserProfile =
            routeNavigation.navigationCallback { theirUserId ->
                push(UserProfileRoute(viewModelContext.userId, roomId, theirUserId))
            },
        onOpenAvatarCutter =
            routeNavigation.navigationCallback { userId, selectedRoomId, avatarPicture ->
                replace(AvatarCutterRoute(userId, selectedRoomId, avatarPicture))
            },
        onOpenPowerLevel =
            routeNavigation.navigationCallback { push(PowerlevelRoute(viewModelContext.userId, roomId)) },
        onOpenMention = { userId, timelineElementMention ->
            viewModelContext.coroutineScope.launch { openMentionLogic.openMention(userId, timelineElementMention) }
        },
    )
}
