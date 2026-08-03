package de.connect2x.trixnity.messenger.internal.factories

import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.internal.logic.AccountSyncStatesLogic
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.initialsync.AccountSync
import de.connect2x.trixnity.messenger.viewmodel.initialsync.SyncViewModel
import de.connect2x.trixnity.messenger.viewmodel.initialsync.SyncViewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * This is similar to the default [SyncViewModelFactory] with the only difference that it actually just waits for all
 * initial syncs to be done instead of managing the initial syncs. The initial syncs have to be controlled via
 * [de.connect2x.trixnity.messenger.internal.workers.InitialSyncWorker]
 */
internal fun WorkerBasedSyncViewModelFactory(accountSyncStatesLogic: AccountSyncStatesLogic): SyncViewModelFactory {
    return WorkerBasedSyncViewModelFactoryImpl(accountSyncStatesLogic = accountSyncStatesLogic)
}

private class WorkerBasedSyncViewModelFactoryImpl(private val accountSyncStatesLogic: AccountSyncStatesLogic) :
    SyncViewModelFactory {
    override fun create(viewModelContext: ViewModelContext, onSyncDone: () -> Unit): SyncViewModel {
        return WorkerBasedSyncViewModelImpl(
            viewModelContext = viewModelContext,
            accountSyncStatesLogic = accountSyncStatesLogic,
        )
    }
}

private class WorkerBasedSyncViewModelImpl(
    viewModelContext: ViewModelContext,
    accountSyncStatesLogic: AccountSyncStatesLogic,
) : ViewModelContext by viewModelContext, SyncViewModel {

    override val accountSyncStates: StateFlow<Map<UserId, AccountSync>> =
        accountSyncStatesLogic
            .accountSyncStates()
            .stateIn(scope = coroutineScope, started = SharingStarted.Eagerly, initialValue = emptyMap())
}
