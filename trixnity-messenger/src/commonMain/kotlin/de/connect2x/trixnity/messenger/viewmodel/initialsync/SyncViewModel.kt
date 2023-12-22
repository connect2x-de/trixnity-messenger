package de.connect2x.trixnity.messenger.viewmodel.initialsync

import de.connect2x.trixnity.messenger.util.IsNetworkAvailable
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.matrixClients
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.utils.io.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.folivo.trixnity.clientserverapi.client.SyncState
import net.folivo.trixnity.core.model.UserId
import org.koin.core.component.get

private val log = KotlinLogging.logger { }

enum class AccountSync {
    INITIAL_SYNC, SYNC, DONE
}

interface SyncViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        onSyncDone: () -> Unit,
    ): SyncViewModel {
        return SyncViewModelImpl(viewModelContext, onSyncDone)
    }

    companion object : SyncViewModelFactory
}

interface SyncViewModel {
    val accountSyncStates: StateFlow<Map<UserId, AccountSync>?>
    fun cancel()
}

open class SyncViewModelImpl(
    viewModelContext: ViewModelContext,
    private val onSyncDone: () -> Unit,
) : SyncViewModel, ViewModelContext by viewModelContext {

    override val accountSyncStates: MutableStateFlow<Map<UserId, AccountSync>?> = MutableStateFlow(null)
    private val isNetworkAvailable = get<IsNetworkAvailable>()
    private val runInitialSync = get<RunInitialSync>()

    protected var syncJob: Job = coroutineScope.launch { doSync() }

    private suspend fun doSync() {
        if (isNetworkAvailable()) {
            coroutineScope {
                val matrixClients = matrixClients.value
                val startSyncState = matrixClients.map { (userId, matrixClient) ->
                    userId to (if (matrixClient.initialSyncDone.value) AccountSync.SYNC else AccountSync.INITIAL_SYNC)
                }.toMap()
                accountSyncStates.value = startSyncState

                matrixClients.forEach { (userId, matrixClient) ->
                    val syncState = matrixClient.syncState.value
                    if (syncState != SyncState.RUNNING && syncState != SyncState.INITIAL_SYNC) {
                        if (startSyncState[userId] != AccountSync.DONE) {
                            launch {
                                log.info { "start initial sync or sync once (${startSyncState[userId]}) for $userId" }
                                val success = runInitialSync(matrixClient)
                                log.info { "finihed initial sync or sync once (${startSyncState[userId]}) for $userId (success=$success)" }
                                accountSyncStates.update {
                                    it.orEmpty() + (userId to AccountSync.DONE)
                                }
                            }
                        }
                    }
                }
            }
            log.info { "initial sync done" }
        }
        onSyncDone()
    }

    // TODO currently canceling a sync is not a good idea, because it can lead to Timeline loops in edge cases
    //  (see also https://gitlab.com/trixnity/trixnity/-/issues/241)
    //  Do we really need this viewmodel? On slow mobile devices it feels a bit weired to wait 10 or more seconds until
    //  the app can be used.
    override fun cancel() {
        syncJob.cancel(CancellationException("User aborted initial small sync."))
        onSyncDone()
    }
}

class PreviewSyncViewModel : SyncViewModel {
    override val accountSyncStates: StateFlow<Map<UserId, AccountSync>?> = MutableStateFlow(
        mapOf(
            UserId("@martin:localhorst.local") to AccountSync.DONE,
            UserId("@martin:loading.local") to AccountSync.SYNC,
            UserId("@martin:local.local") to AccountSync.INITIAL_SYNC,
        )
    )

    override fun cancel() {
    }
}
