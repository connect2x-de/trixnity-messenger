package de.connect2x.trixnity.messenger.viewmodel.initialsync

import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.matrixClients
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private val log = KotlinLogging.logger { }

class IosInitialSyncViewModel(
    private val viewModelContext: ViewModelContext,
    private val onInitialSyncDone: suspend () -> Unit,
) : SyncViewModel, ViewModelContext by viewModelContext {

    // TODO this is not good practise and should be evaluated on a real device
    private val scope: CoroutineScope =
        CoroutineScope(coroutineScope.coroutineContext) // no lifecycle as this _has to_ run in the background
    private var job: Job? = null

    fun doInitialSync() {
        log.info { "initial sync" }
        job = scope.launch {
            matrixClients.value.forEach { matrixClient ->
                val result = InitialSync.run(matrixClient)
                if (result) {
                    log.info { "initial sync successful" }
                } else {
                    log.error { "initial sync not completed successfully" }
                }
            }

            onInitialSyncDone()
        }
    }

    fun stopInitialSync() {
        job?.cancel()
    }

    override val accountSyncStates: StateFlow<Map<String, AccountSync>>
        get() = TODO("Not yet implemented")

    override fun cancel() {
        TODO("Not yet implemented")
    }

}
