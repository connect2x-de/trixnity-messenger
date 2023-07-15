package de.connect2x.trixnity.messenger.viewmodel.initialsync

import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.initialsync.AccountSyncState.DONE
import de.connect2x.trixnity.messenger.viewmodel.initialsync.AccountSyncState.RUNNING
import de.connect2x.trixnity.messenger.viewmodel.initialsync.InitialSyncState.NOT_DONE
import de.connect2x.trixnity.messenger.viewmodel.util.IsNetworkAvailable
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.utils.io.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.get

private val log = KotlinLogging.logger { }

data class AccountSync(
    val initialSyncState: InitialSyncState,
    val accountSyncState: AccountSyncState,
)

enum class AccountSyncState {
    RUNNING, DONE
}

interface SyncViewModelFactory {
    fun newSyncViewModel(
        viewModelContext: ViewModelContext,
        accountNames: Map<String, InitialSyncState>,
        onSyncDone: () -> Unit,
    ): SyncViewModel {
        return SyncViewModelImpl(viewModelContext, accountNames, onSyncDone)
    }
}

interface SyncViewModel {
    val accountSyncStates: StateFlow<Map<String, AccountSync>>
    fun cancel()
}

open class SyncViewModelImpl(
    viewModelContext: ViewModelContext,
    private val accountNames: Map<String, InitialSyncState>,
    private val onSyncDone: () -> Unit,
) : SyncViewModel, ViewModelContext by viewModelContext {

    override val accountSyncStates: MutableStateFlow<Map<String, AccountSync>> =
        MutableStateFlow(accountNames.map { (accountName, initialSyncState) ->
            accountName to AccountSync(initialSyncState, RUNNING)
        }.toMap())
    private val isNetworkAvailable = get<IsNetworkAvailable>()

    protected var syncJob: Job = coroutineScope.launch {
        doSync()
    }

    private suspend fun doSync() {
        if (isNetworkAvailable()) {
            coroutineScope {
                accountNames.entries.map { (accountName, initialSyncState) ->
                    if (initialSyncState == NOT_DONE) {
                        launch {
                            log.info { "initial sync for $accountName" }
                            val success =
                                get<RunInitialSync>()(accountName).first()
                            log.info { "initial sync done ($accountName): $success" }
                            accountSyncStates.update {
                                it - accountName + (accountName to AccountSync(initialSyncState, DONE))
                            }
                        }
                    } else {
                        launch {
                            log.debug { "start small sync for $accountName" }
                            val success =
                                get<RunInitialSync>()(accountName).first()
                            log.debug { "done small sync ($accountName): $success" }
                            accountSyncStates.update {
                                it - accountName + (accountName to AccountSync(initialSyncState, DONE))
                            }
                        }
                    }
                }.forEach { it.join() }
            }
        }
        log.info { "initial sync done"}
        onSyncDone()
    }

    override fun cancel() {
        syncJob.cancel(CancellationException("User aborted initial small sync."))
        onSyncDone()
    }
}

class PreviewSyncViewModel : SyncViewModel {
    override val accountSyncStates: MutableStateFlow<Map<String, AccountSync>> = MutableStateFlow(
        mapOf(
            "@martin:localhorst.local" to AccountSync(NOT_DONE, RUNNING),
            "@martin:loading.local" to AccountSync(InitialSyncState.DONE, RUNNING),
            "@martin:local.local" to AccountSync(InitialSyncState.DONE, DONE)
        )
    )

    override fun cancel() {
    }
}
