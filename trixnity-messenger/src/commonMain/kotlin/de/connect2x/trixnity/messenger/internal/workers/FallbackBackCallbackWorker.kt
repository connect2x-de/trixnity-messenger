package de.connect2x.trixnity.messenger.internal.workers

import de.connect2x.trixnity.messenger.Worker
import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.routes.TimelineRoute
import de.connect2x.trixnity.messenger.internal.routes.roomlist.RoomListRoute
import de.connect2x.trixnity.messenger.internal.util.MinimizeAppIfPossible
import de.connect2x.trixnity.messenger.util.BackCallback
import de.connect2x.trixnity.messenger.util.BackHandler
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest

internal interface FallbackBackCallbackWorker : Worker

internal fun FallbackBackCallbackWorker(
    routeNavigation: RouteNavigation,
    backHandler: BackHandler,
    minimizeAppIfPossible: MinimizeAppIfPossible,
): FallbackBackCallbackWorker {
    return FallbackBackCallbackWorkerImpl(
        routeNavigation = routeNavigation,
        backHandler = backHandler,
        minimizeAppIfPossible = minimizeAppIfPossible,
    )
}

private class FallbackBackCallbackWorkerImpl(
    private val routeNavigation: RouteNavigation,
    private val backHandler: BackHandler,
    private val minimizeAppIfPossible: MinimizeAppIfPossible,
) : FallbackBackCallbackWorker {

    override suspend fun doWork() {
        backHandler.asFlow().collectLatest { updateNavigation() }
    }

    private fun updateNavigation() {
        routeNavigation.updateNavigation {
            when {
                items.any { it is TimelineRoute } -> items = listOf(RoomListRoute)
                items.last() == RoomListRoute -> minimizeAppIfPossible.invoke()
            }
        }
    }
}

private fun BackHandler.asFlow(): Flow<Unit> {
    return callbackFlow {
            val callback = BackCallback(-1) { trySend(Unit) }
            registerBackCallback(callback)
            awaitClose { unregisterCallback(callback) }
        }
        .buffer(Channel.CONFLATED)
}
