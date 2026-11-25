package de.connect2x.trixnity.messenger.media

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.lognity.api.logger.debug
import de.connect2x.lognity.api.logger.warn
import js.reflect.Reflect
import kotlin.js.ExperimentalWasmJsInterop
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import web.dom.Document
import web.dom.document
import web.dom.visibilityChangeEvent
import web.events.addHandler
import web.navigator.Navigator
import web.navigator.navigator
import web.wakelock.WakeLockSentinel
import web.wakelock.release
import web.wakelock.request

private val log = Logger("de.connect2x.trixnity.messenger.media.WebScreenWakeLock")

internal fun ScreenWakeLock(): ScreenWakeLock {
    return WebScreenWakeLock(document, navigator)
}

private class WebScreenWakeLock(private val document: Document, private val navigator: Navigator) : ScreenWakeLock {
    @OptIn(ExperimentalWasmJsInterop::class)
    override suspend fun hold() {
        if (!Reflect.has(navigator, "wakeLock")) return
        documentVisibility().collectLatest { visible -> if (visible) holdWhileVisible() }
    }

    private fun documentVisibility(): Flow<Boolean> {
        return callbackFlow {
                val removeListener = document.visibilityChangeEvent.addHandler { trySend(!document.hidden) }
                trySend(!document.hidden)
                awaitClose { removeListener() }
            }
            .distinctUntilChanged()
    }

    private suspend fun holdWhileVisible() {
        val lock = requestWakeLock() ?: return
        try {
            awaitCancellation()
        } finally {
            releaseWakeLock(lock)
        }
    }

    private suspend fun requestWakeLock(): WakeLockSentinel? {
        return withContext(NonCancellable) {
            try {
                navigator.wakeLock.request()
            } catch (error: Throwable) {
                log.debug(error) { "Could not acquire screen wake lock" }
                null
            }
        }
    }

    private suspend fun releaseWakeLock(lock: WakeLockSentinel) {
        withContext(NonCancellable) {
            try {
                lock.release()
            } catch (error: Throwable) {
                log.warn(error) { "Could not release screen wake lock" }
            }
        }
    }
}
