package de.connect2x.trixnity.messenger.internal.adapters

import de.connect2x.trixnity.messenger.internal.logic.SelectedRoomIdLogic
import de.connect2x.trixnity.messenger.internal.logic.SendLogsLogic
import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.ViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.navigation.closeRoom
import de.connect2x.trixnity.messenger.internal.navigation.navigationCallback
import de.connect2x.trixnity.messenger.internal.navigation.openRoom
import de.connect2x.trixnity.messenger.internal.navigation.replace
import de.connect2x.trixnity.messenger.internal.routes.roomlist.AccountsRoute
import de.connect2x.trixnity.messenger.internal.routes.roomlist.AppInfoRoute
import de.connect2x.trixnity.messenger.internal.routes.roomlist.CreateNewChatRoute
import de.connect2x.trixnity.messenger.internal.routes.roomlist.UserSettingsRoute
import de.connect2x.trixnity.messenger.internal.routes.selfverification.SelfVerificationRoute
import de.connect2x.trixnity.messenger.internal.routes.selfverification.SelfVerificationRouteMarker
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListViewModel
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListViewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.parameter.ParametersHolder

internal class RoomListViewModelFactoryAdapter(
    private val factory: RoomListViewModelFactory,
    private val routeNavigation: RouteNavigation,
    private val sendLogsLogic: SendLogsLogic,
    private val selectedRoomIdLogic: SelectedRoomIdLogic,
) : ViewModelFactoryAdapter<RoomListViewModel> {

    override fun create(parameters: ParametersHolder): RoomListViewModel {
        val viewModelContext = parameters.get<ViewModelContext>().childContext("RoomList")

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
            selectedRoomId = selectedRoomId,
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
                routeNavigation.navigationCallback {
                    closeRoom()
                    // TODO: possiblyStartAccountSetup???
                },
            onStartVerification =
                routeNavigation.navigationCallback { userId ->
                    replace<SelfVerificationRouteMarker>(SelfVerificationRoute(userId))
                },
            onCloseRoom = routeNavigation.navigationCallback { closeRoom() },
        )
    }
}
