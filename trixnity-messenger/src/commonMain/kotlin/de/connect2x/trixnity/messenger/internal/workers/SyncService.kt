package de.connect2x.trixnity.messenger.internal.workers

import com.arkivanov.essenty.lifecycle.Lifecycle
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.core.model.events.m.Presence
import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.MatrixMessengerAccountSettings
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.Worker
import de.connect2x.trixnity.messenger.viewmodel.util.scopedCollectLatest
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

internal interface SyncService : Worker

internal fun SyncService(
    matrixClients: MatrixClients,
    messengerSettingsHolder: MatrixMessengerSettingsHolder,
    lifecycle: Lifecycle,
): SyncService {
    return SyncServiceImpl(
        matrixClients = matrixClients,
        messengerSettingsHolder = messengerSettingsHolder,
        lifecycle = lifecycle,
    )
}

private class SyncServiceImpl(
    private val matrixClients: MatrixClients,
    private val messengerSettingsHolder: MatrixMessengerSettingsHolder,
    private val lifecycle: Lifecycle,
) : SyncService {

    override suspend fun doWork() = coroutineScope {
        lifecycle.asFlow().map { it >= Lifecycle.State.STARTED }.distinctUntilChanged().collectLatest(::onStateChange)
    }

    private suspend fun onStateChange(isAtLeastStarted: Boolean) {
        if (isAtLeastStarted) start() else stop()
    }

    private suspend fun start() {
        syncMatrixClients(matrixClients, messengerSettingsHolder)
    }

    private suspend fun stop() {
        matrixClients.value.forEach { (_, matrixClient) -> matrixClient.stopSync() }
    }

    private suspend fun syncMatrixClients(
        matrixClients: MatrixClients,
        messengerSettingsHolder: MatrixMessengerSettingsHolder,
    ) {
        matrixClients.scopedCollectLatest { matrixClients ->
            matrixClients
                .map { (_, matrixClient) -> async { syncMatrixClient(matrixClient, messengerSettingsHolder) } }
                .awaitAll()
        }
    }

    private suspend fun syncMatrixClient(
        matrixClient: MatrixClient,
        messengerSettingsHolder: MatrixMessengerSettingsHolder,
    ) {
        matrixClient.initialSyncDone.first { it }

        messengerSettingsHolder[matrixClient.userId]
            .mapNotNull { it.presence() }
            .distinctUntilChanged()
            .collectLatest {
                log.info { "Starting Sync (userId=${matrixClient.userId}, presence=$it" }
                matrixClient.startSync(presence = it)
            }
    }

    private fun MatrixMessengerAccountSettings?.presence(): Presence {
        return this?.base?.presenceIsPublic?.takeIf { it }?.let { Presence.ONLINE } ?: Presence.OFFLINE
    }

    private companion object {
        private val log = Logger("SyncService")
    }
}

private fun Lifecycle.asFlow(): Flow<Lifecycle.State> {
    return callbackFlow {
            val callbacks = TrySendCallbacks(trySend = channel::trySend, onDestroy = channel::close)

            subscribe(callbacks)
            awaitClose { unsubscribe(callbacks) }
        }
        .buffer(Channel.CONFLATED)
        .distinctUntilChanged()
}

private class TrySendCallbacks(private val trySend: (Lifecycle.State) -> Unit, private val onDestroy: () -> Unit) :
    Lifecycle.Callbacks {
    override fun onCreate() {
        trySend(Lifecycle.State.CREATED)
    }

    override fun onStart() {
        trySend(Lifecycle.State.STARTED)
    }

    override fun onResume() {
        trySend(Lifecycle.State.RESUMED)
    }

    override fun onPause() {
        trySend(Lifecycle.State.STARTED)
    }

    override fun onStop() {
        trySend(Lifecycle.State.CREATED)
    }

    override fun onDestroy() {
        trySend(Lifecycle.State.DESTROYED)
        onDestroy.invoke()
    }
}
