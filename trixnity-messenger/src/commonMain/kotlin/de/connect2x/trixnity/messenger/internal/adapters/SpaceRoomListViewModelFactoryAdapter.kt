package de.connect2x.trixnity.messenger.internal.adapters

import de.connect2x.trixnity.messenger.internal.logic.SelectedRoomIdLogic
import de.connect2x.trixnity.messenger.internal.logic.SendLogsLogic
import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.ViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.navigation.closeRoom
import de.connect2x.trixnity.messenger.internal.navigation.navigationCallback
import de.connect2x.trixnity.messenger.internal.navigation.openRoom
import de.connect2x.trixnity.messenger.internal.navigation.openSpaceExplore
import de.connect2x.trixnity.messenger.internal.navigation.openSpaceItem
import de.connect2x.trixnity.messenger.internal.navigation.removeSpace
import de.connect2x.trixnity.messenger.internal.navigation.replace
import de.connect2x.trixnity.messenger.internal.navigation.returnHomeForUnavailableSpace
import de.connect2x.trixnity.messenger.internal.navigation.selectSpaceAccount
import de.connect2x.trixnity.messenger.internal.routes.TimelineRoute
import de.connect2x.trixnity.messenger.internal.routes.roomlist.AccountsRoute
import de.connect2x.trixnity.messenger.internal.routes.roomlist.AppInfoRoute
import de.connect2x.trixnity.messenger.internal.routes.roomlist.CreateNewChatRoute
import de.connect2x.trixnity.messenger.internal.routes.roomlist.UserSettingsRoute
import de.connect2x.trixnity.messenger.internal.routes.selfverification.SelfVerificationRoute
import de.connect2x.trixnity.messenger.internal.routes.selfverification.SelfVerificationRouteMarker
import de.connect2x.trixnity.messenger.internal.routes.spaces.SpaceExploreRoute
import de.connect2x.trixnity.messenger.internal.routes.spaces.SpaceRoomListRoute
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.spaces.RoomListSpace
import de.connect2x.trixnity.messenger.viewmodel.spaces.SpaceRoomListViewModel
import de.connect2x.trixnity.messenger.viewmodel.spaces.SpaceRoomListViewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.parameter.ParametersHolder

internal class SpaceRoomListViewModelFactoryAdapter(
    private val factory: SpaceRoomListViewModelFactory,
    private val routeNavigation: RouteNavigation,
    private val sendLogsLogic: SendLogsLogic,
    private val selectedRoomIdLogic: SelectedRoomIdLogic,
) : ViewModelFactoryAdapter<SpaceRoomListViewModel> {

    override fun create(parameters: ParametersHolder): SpaceRoomListViewModel {
        val route = parameters.get<SpaceRoomListRoute>()
        val viewModelContext = parameters.get<ViewModelContext>().childContext("SpaceRoomList")

        val selectedRoomId =
            selectedRoomIdLogic
                .selectedRoomIdFlow()
                .stateIn(
                    scope = viewModelContext.coroutineScope,
                    started = SharingStarted.Eagerly,
                    initialValue = selectedRoomIdLogic.selectedRoomId,
                )

        return factory.create(
            viewModelContext = viewModelContext,
            space = RoomListSpace(route.userId, route.spaceId),
            onSelect =
                routeNavigation.navigationCallback { item ->
                    openSpaceItem(item, showExplore = items.any { it is SpaceExploreRoute })
                },
            onBack = routeNavigation.navigationCallback { removeSpace(route) },
            selectedRoomId = selectedRoomId,
            exploreSelected =
                routeNavigation.routes
                    .map { SpaceExploreRoute(route.userId, route.spaceId) in it }
                    .stateIn(
                        viewModelContext.coroutineScope,
                        SharingStarted.Eagerly,
                        SpaceExploreRoute(route.userId, route.spaceId) in routeNavigation.routes.value,
                    ),
            onOpenExplore = routeNavigation.navigationCallback { openSpaceExplore(route.userId, route.spaceId) },
            onRoomSelected = routeNavigation.navigationCallback { userId, roomId -> openRoom(userId, roomId) },
            onStartCreateNewRoom = routeNavigation.navigationCallback { userId -> push(CreateNewChatRoute(userId)) },
            onUserSettingsSelected = routeNavigation.navigationCallback { push(UserSettingsRoute) },
            onShowAccounts = routeNavigation.navigationCallback { push(AccountsRoute) },
            onOpenAppInfo = routeNavigation.navigationCallback { push(AppInfoRoute) },
            onSendLogs =
                routeNavigation.navigationCallback {
                    viewModelContext.coroutineScope.launch { sendLogsLogic.sendLogs() }
                },
            onAccountSelected =
                routeNavigation.navigationCallback { userId ->
                    selectSpaceAccount(
                        route,
                        userId,
                        showExplore = items.any { it is SpaceExploreRoute || it is TimelineRoute },
                    )
                },
            onSpaceUnavailable = routeNavigation.navigationCallback { returnHomeForUnavailableSpace(route) },
            onStartVerification =
                routeNavigation.navigationCallback { userId ->
                    replace<SelfVerificationRouteMarker>(SelfVerificationRoute(userId))
                },
            onCloseRoom = routeNavigation.navigationCallback { closeRoom() },
        )
    }
}
