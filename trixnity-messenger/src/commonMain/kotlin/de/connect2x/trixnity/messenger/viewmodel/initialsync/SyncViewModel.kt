package de.connect2x.trixnity.messenger.viewmodel.initialsync

import de.connect2x.trixnity.messenger.util.IsNetworkAvailable
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.matrixClients
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import de.connect2x.trixnity.clientserverapi.client.SyncState
import de.connect2x.trixnity.core.model.UserId
import org.koin.core.component.get

enum class AccountSync {
    INITIAL_SYNC, DONE,
}

interface SyncViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        onSyncDone: () -> Unit,
    ): SyncViewModel =
        SyncViewModelImpl(viewModelContext, onSyncDone)

    companion object : SyncViewModelFactory
}

interface SyncViewModel {
    val accountSyncStates: StateFlow<Map<UserId, AccountSync>?>
}

open class SyncViewModelImpl(
    viewModelContext: ViewModelContext,
    private val onSyncDone: () -> Unit,
) : SyncViewModel, ViewModelContext by viewModelContext {

    override val accountSyncStates: MutableStateFlow<Map<UserId, AccountSync>?> = MutableStateFlow(null)
    private val isNetworkAvailable = get<IsNetworkAvailable>()
    private val runInitialSync = get<RunInitialSync>()

    init {
        coroutineScope.launch { doSync() }
    }

    private suspend fun doSync() {
        if (isNetworkAvailable()) {
            coroutineScope {
                val matrixClients = matrixClients.value
                val initialSyncUsers = matrixClients
                    .filterNot { (_, matrixClient) ->
                        matrixClient.initialSyncDone.value
                    }.mapValues { AccountSync.INITIAL_SYNC }
                accountSyncStates.value = initialSyncUsers

                matrixClients
                    .filterKeys { initialSyncUsers.containsKey(it) }
                    .forEach { (userId, matrixClient) ->
                        val syncState = matrixClient.syncState.value
                        if (syncState != SyncState.RUNNING && syncState != SyncState.INITIAL_SYNC) {
                            launch {
                                log.info { "start initial sync for $userId" }
                                val success = runInitialSync(matrixClient)
                                log.info { "finihed initial sync for $userId (success=$success)" }
                            }
                            launch {
                                matrixClient.initialSyncDone.first { it }
                                accountSyncStates.update {
                                    it.orEmpty() + (userId to AccountSync.DONE)
                                }
                            }
                        }
                    }
            }
            log.info { "initial sync done" }
        } else log.warn { "unable to sync due to missing network connection" }
        onSyncDone()
    }
}

class PreviewSyncViewModel : SyncViewModel {
    override val accountSyncStates: StateFlow<Map<UserId, AccountSync>?> = MutableStateFlow(
        mapOf(
            UserId("@martin:localhorst.local") to AccountSync.DONE,
            UserId("@martin:local.local") to AccountSync.INITIAL_SYNC,
        )
    )
}
