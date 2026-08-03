package de.connect2x.trixnity.messenger.internal.workers

import de.connect2x.trixnity.messenger.Worker
import de.connect2x.trixnity.messenger.internal.logic.AccountSyncStatesLogic
import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.clear
import de.connect2x.trixnity.messenger.internal.navigation.replace
import de.connect2x.trixnity.messenger.internal.routes.SyncRoute
import de.connect2x.trixnity.messenger.internal.routes.roomlist.RoomListRoute
import de.connect2x.trixnity.messenger.viewmodel.initialsync.AccountSync
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal interface ShowSyncWorker : Worker

internal fun ShowSyncWorker(
    routeNavigation: RouteNavigation,
    accountSyncStatesLogic: AccountSyncStatesLogic,
): ShowSyncWorker {
    return ShowSyncWorkerImpl(routeNavigation = routeNavigation, accountSyncStatesLogic = accountSyncStatesLogic)
}

private class ShowSyncWorkerImpl(
    private val routeNavigation: RouteNavigation,
    private val accountSyncStatesLogic: AccountSyncStatesLogic,
) : ShowSyncWorker {
    override suspend fun doWork() {
        computeAction(routeNavigation = routeNavigation, accountSyncStatesLogic = accountSyncStatesLogic)
            .collect(::applyAction)
    }

    private fun applyAction(action: SyncRouteAction) {
        when (action) {
            SyncRouteAction.ShowSync -> routeNavigation.updateNavigation { replace<SyncRoute>(SyncRoute) }
            SyncRouteAction.HideSync -> routeNavigation.updateNavigation { clear<SyncRoute>() }
            SyncRouteAction.None -> {}
        }
    }
}

private fun computeAction(
    routeNavigation: RouteNavigation,
    accountSyncStatesLogic: AccountSyncStatesLogic,
): Flow<SyncRouteAction> {
    val isRoomListOpen = routeNavigation.routes.map { it.lastOrNull() is RoomListRoute }
    val needsInitialSync =
        accountSyncStatesLogic.accountSyncStates().map { accountSyncStates ->
            accountSyncStates.values.any { it == AccountSync.INITIAL_SYNC }
        }

    return combine(isRoomListOpen, needsInitialSync, ::computeAction).distinctUntilChanged()
}

private fun computeAction(isRoomListOpen: Boolean, needsInitialSync: Boolean): SyncRouteAction {
    return when {
        isRoomListOpen && needsInitialSync -> SyncRouteAction.ShowSync
        !needsInitialSync -> SyncRouteAction.HideSync
        else -> SyncRouteAction.None
    }
}

private sealed interface SyncRouteAction {
    data object ShowSync : SyncRouteAction

    data object HideSync : SyncRouteAction

    data object None : SyncRouteAction
}
