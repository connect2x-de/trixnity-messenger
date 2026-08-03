package de.connect2x.trixnity.messenger.internal.workers

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.clientserverapi.client.SyncState
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.Worker
import de.connect2x.trixnity.messenger.util.IsNetworkAvailable
import de.connect2x.trixnity.messenger.viewmodel.initialsync.RunInitialSync
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

internal interface InitialSyncWorker : Worker

internal fun InitialSyncWorker(
    matrixClients: MatrixClients,
    isNetworkAvailable: IsNetworkAvailable,
    runInitialSync: RunInitialSync,
): InitialSyncWorker {
    return InitialSyncWorkerImpl(
        matrixClients = matrixClients,
        isNetworkAvailable = isNetworkAvailable,
        runInitialSync = runInitialSync,
    )
}

private class InitialSyncWorkerImpl(
    private val matrixClients: MatrixClients,
    private val isNetworkAvailable: IsNetworkAvailable,
    private val runInitialSync: RunInitialSync,
) : InitialSyncWorker {

    override suspend fun doWork() {
        nextMatrixClientToSyncFlow().collect(::runInitialSyncFor)
    }

    private suspend fun runInitialSyncFor(matrixClient: MatrixClient) {
        log.info { "start initial sync for ${matrixClient.userId}" }
        val success = runInitialSync(matrixClient)
        log.info { "finished initial sync for ${matrixClient.userId} (success=$success)" }
    }

    private fun nextMatrixClientToSyncFlow(): Flow<MatrixClient> {
        return matrixClientToSyncFlow()
            .combine(isNetworkAvailableFlow()) { matrixClient, networkAvailable ->
                matrixClient.takeIf { networkAvailable }
            }
            .filterNotNull()
    }

    private fun matrixClientToSyncFlow(): Flow<MatrixClient> {
        return combineForEachClient { matrixClient ->
                shouldSyncFlow(matrixClient).map { shouldSync -> matrixClient.takeIf { shouldSync } }
            }
            .mapNotNull { clientsByUserId -> clientsByUserId.values.filterNotNull().firstOrNull() }
    }

    private fun shouldSyncFlow(matrixClient: MatrixClient): Flow<Boolean> {
        return combine(matrixClient.syncState, matrixClient.initialSyncDone) { syncState, initialSyncDone ->
            !initialSyncDone && syncState != SyncState.RUNNING && syncState != SyncState.INITIAL_SYNC
        }
    }

    private fun isNetworkAvailableFlow(): Flow<Boolean> {
        return flow {
                while (true) {
                    emit(isNetworkAvailable())
                    delay(5.seconds)
                }
            }
            .distinctUntilChanged()
    }

    private fun <T> combineForEachClient(valueFlow: (MatrixClient) -> Flow<T>): Flow<Map<UserId, T>> {
        @OptIn(ExperimentalCoroutinesApi::class)
        return matrixClients.flatMapLatest { clients ->
            if (clients.isEmpty()) {
                flowOf(emptyMap())
            } else {
                combine(clients.map { (userId, matrixClient) -> valueFlow(matrixClient).map { userId to it } }) {
                    it.toMap()
                }
            }
        }
    }

    private companion object {
        private val log = Logger("de.connect2x.trixnity.messenger.internal.workers.InitialSyncWorker")
    }
}
