package de.connect2x.trixnity.messenger.internal.workers

import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.Worker
import de.connect2x.trixnity.messenger.internal.logic.AccountSetupFinishedLogic
import de.connect2x.trixnity.messenger.internal.logic.AccountSyncStatesLogic
import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.routes.AccountSetupRoute
import de.connect2x.trixnity.messenger.internal.routes.roomlist.RoomListRoute
import de.connect2x.trixnity.messenger.viewmodel.initialsync.AccountSync
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

internal interface ShowAccountSetupWorker : Worker

internal fun ShowAccountSetupWorker(
    routeNavigation: RouteNavigation,
    accountSyncStatesLogic: AccountSyncStatesLogic,
    accountSetupFinishedLogic: AccountSetupFinishedLogic,
): ShowAccountSetupWorker {
    return ShowAccountSetupWorkerImpl(
        routeNavigation = routeNavigation,
        accountSyncStatesLogic = accountSyncStatesLogic,
        accountSetupFinishedLogic = accountSetupFinishedLogic,
    )
}

private class ShowAccountSetupWorkerImpl(
    private val routeNavigation: RouteNavigation,
    private val accountSyncStatesLogic: AccountSyncStatesLogic,
    private val accountSetupFinishedLogic: AccountSetupFinishedLogic,
) : ShowAccountSetupWorker {
    override suspend fun doWork() {
        val isRoomListOpen = routeNavigation.routes.map { it.lastOrNull() is RoomListRoute }
        val isInitialSyncDone =
            accountSyncStatesLogic.accountSyncStates().map { accountSyncStates ->
                accountSyncStates.values.all { it == AccountSync.DONE }
            }
        val userIdsToSetup =
            accountSetupFinishedLogic.accountSetupFinished().map { accountSetupFinished ->
                accountSetupFinished.filterValues { !it }.keys
            }

        combine(isRoomListOpen, isInitialSyncDone, userIdsToSetup, ::nextUserId)
            .filterNotNull()
            .distinctUntilChanged()
            .collect(::showAccountSetup)
    }

    private fun nextUserId(isRoomListOpen: Boolean, isInitialSyncDone: Boolean, userIdsToSetup: Set<UserId>): UserId? {
        return if (!isRoomListOpen || !isInitialSyncDone) null else userIdsToSetup.firstOrNull()
    }

    private fun showAccountSetup(userId: UserId) {
        routeNavigation.updateNavigation { push(AccountSetupRoute(userId)) }
    }
}
