package de.connect2x.messenger.compose.view

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import web.broadcast.BroadcastChannel
import web.events.EventHandler
import web.window.window
import kotlin.time.Duration.Companion.seconds

private val log = KotlinLogging.logger {}

private const val BROADCAST_PING: String = "ping!"
private const val BROADCAST_PONG: String = "pong!"

private val pingPongChannel: BroadcastChannel = BroadcastChannel("trixnity-messenger-pingpong").apply {
    window.onbeforeunload = EventHandler { close() }
    log.info { "Created ping-pong broadcast channel" }
}

internal suspend fun isPrimaryInstance(): Boolean {
    val isSecondary = CompletableDeferred<Unit>()
    pingPongChannel.postMessage(BROADCAST_PING)
    pingPongChannel.onmessage = EventHandler { event ->
        when (event.data.toString()) {
            // If the message is a ping, we simply respond with a pong
            BROADCAST_PING -> {
                log.info { "Got ping from another instance!" }
                pingPongChannel.postMessage(BROADCAST_PONG)
            }
            // If the message is a pong, we know there's already a running instance
            BROADCAST_PONG -> {
                log.info { "Got pong from another instance!" }
                isSecondary.complete(Unit)
            }
        }
    }
    return withTimeoutOrNull(2.seconds) {
        isSecondary.await()
        false
    } ?: true
}
