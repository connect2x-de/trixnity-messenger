@file:OptIn(ExperimentalWasmJsInterop::class)

package de.connect2x.trixnity.messenger.compose.view

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.messenger.MatrixMessengerBaseConfiguration
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import web.broadcast.BroadcastChannel
import web.events.EventHandler
import web.window.window

private const val BROADCAST_PING: String = "ping!"
private const val BROADCAST_PONG: String = "pong!"

private val log: Logger = Logger("de.connect2x.trixnity.messenger.compose.view.isPrimaryInstanceKt")
private var pingPongChannel: BroadcastChannel? = null

private fun getOrCreatePingPongChannel(appId: String): BroadcastChannel {
    if (pingPongChannel == null) {
        pingPongChannel =
            BroadcastChannel("$appId-ping-pong").apply {
                window.onbeforeunload = EventHandler { close() }
                log.info { "Created ping-pong broadcast channel" }
            }
    }
    return requireNotNull(pingPongChannel) { "Could not obtain ping-pong channel" }
}

internal suspend fun isPrimaryInstance(config: MatrixMessengerBaseConfiguration): Boolean {
    val pingPongChannel = getOrCreatePingPongChannel(config.appId)
    val isSecondary = CompletableDeferred<Unit>()
    pingPongChannel.postMessage(BROADCAST_PING)
    pingPongChannel.onmessage = EventHandler { event ->
        when (event.data.toString()) {
            // If the message is a ping, we simply respond with a pong
            BROADCAST_PING -> {
                log.info { "Got ping from another ${config.appName} instance!" }
                pingPongChannel.postMessage(BROADCAST_PONG)
            }
            // If the message is a pong, we know there's already a running instance
            BROADCAST_PONG -> {
                log.info { "Got pong from another ${config.appName} instance!" }
                isSecondary.complete(Unit)
            }
        }
    }
    return withTimeoutOrNull(200.milliseconds) {
        isSecondary.await()
        false
    } ?: true
}
