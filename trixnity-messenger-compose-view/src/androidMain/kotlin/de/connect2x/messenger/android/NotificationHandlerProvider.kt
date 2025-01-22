package de.connect2x.messenger.android

import android.content.Context
import android.content.Intent
import android.net.Uri
import de.connect2x.sysnotify.NotificationHandler
import de.connect2x.sysnotify.create
import de.connect2x.trixnity.messenger.PushMode
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerConfiguration
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.folivo.trixnity.core.model.UserId

interface NotificationHandlerProvider : StateFlow<Map<UserId, NotificationHandler>>, AutoCloseable {
    fun updateHandlers(
        context: Context,
        config: MatrixMultiMessengerConfiguration,
        pushModes: Map<UserId, PushMode>
    )
}

internal class NotificationHandlerProviderImpl(
    private val handlerFlow: MutableStateFlow<Map<UserId, NotificationHandler>> = MutableStateFlow(emptyMap())
) : NotificationHandlerProvider, StateFlow<Map<UserId, NotificationHandler>> by handlerFlow {
    private val log = KotlinLogging.logger { }

    override fun updateHandlers(
        context: Context,
        config: MatrixMultiMessengerConfiguration,
        pushModes: Map<UserId, PushMode>
    ) {
        // This implementation discards all existing handlers and recreates them from scratch afterwards -
        // not the most efficient but the easiest to maintain/understand; no need for complexity here
        log.debug { "Updating notification handlers for new push modes" }
        handlerFlow.value.values.forEach { it.close() }
        handlerFlow.value = pushModes.filter { it.value == PushMode.PUSH }.map {
            val userId = it.key
            val channelId = pushChannelId(userId, config)
            log.debug { "Creating notification handler with channel ID $channelId" }
            userId to NotificationHandler.create(
                name = config.appName,
                id = channelId,
                contextGetter = { context },
                activationIntent = { _, notification ->
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("${config.urlProtocol}://localhost/room/${notification.userData}"),
                    )
                }
            )
        }.toMap()
    }

    override fun close() {
        log.debug { "Discarding all notification handlers" }
        handlerFlow.value.values.forEach { it.close() }
        handlerFlow.value = emptyMap()
    }
}
