package de.connect2x.messenger.compose.view

import de.connect2x.messenger.compose.view.notifications.NotificationHandlerProvider
import de.connect2x.messenger.compose.view.notifications.notificationsModule
import de.connect2x.sysnotify.Notification
import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.MatrixMessenger
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessenger
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerConfiguration
import de.connect2x.trixnity.messenger.multi.create
import de.connect2x.trixnity.messenger.viewmodel.util.RoomName
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.folivo.trixnity.client.notification
import net.folivo.trixnity.clientserverapi.model.push.PusherData
import net.folivo.trixnity.clientserverapi.model.push.SetPushers
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.idOrNull
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.core.model.events.roomIdOrNull
import org.koin.core.Koin
import kotlin.time.Duration.Companion.seconds

private val log = KotlinLogging.logger { }

val notificationTokenFlow: MutableStateFlow<String?> = MutableStateFlow(null)

fun setNotificationToken(token: String) {
    notificationTokenFlow.value = token
}

private fun createSetPushersRequest(userId: UserId, deviceId: String, di: Koin, token: String): SetPushers.Request {
    val configuration = di.get<MatrixMultiMessengerConfiguration>()
    return SetPushers.Request.Set(
        appDisplayName = "${configuration.appName} (iOS)",
        appId = "${configuration.appId}.ios",
        data = PusherData(
            format = "event_id_only",
            url = configuration.pushUrl,
            customFields = JsonObject(
                mapOf(
                    "default_payload" to JsonObject(
                        mapOf(
                            "aps" to JsonObject(
                                mapOf(
                                    "content-available" to JsonPrimitive(1)
                                )
                            ),
                            "user_id" to JsonPrimitive(userId.toString())
                        )
                    )
                )
            )
        ),
        deviceDisplayName = "$userId ($deviceId)",
        kind = "http",
        lang = "de",
        pushkey = token
    )
}

internal suspend fun setPusher(messenger: MatrixMessenger, userId: UserId) {
    val token = withTimeoutOrNull(30.seconds) {
        requireNotNull(notificationTokenFlow.first { it != null }).also {
            notificationTokenFlow.value = null
        }
    }
    if (token == null) {
        log.error { "Unable to set pusher: No pusher token received" }
        return
    }

    // Set the pusher
    log.debug { "Set pushers for MatrixClients that currently have their push mode active (token = $token)" }
    val client = messenger.di.get<MatrixClients>().filter { it[userId] != null }.first()[userId]
    client?.api?.push?.setPushers(createSetPushersRequest(userId, client.deviceId, messenger.di, token))
        ?.fold(
            onSuccess = {
                log.debug { "Set pushers for $userId successfully" }
            },
            onFailure = {
                log.error(it) { "Failed to set pushers for $userId" }
            }
        )
}

@Suppress("Unused")
fun handleNotification(userId: String, roomId: String, eventId: String) {
    val userId = UserId(userId)
    val roomId = RoomId(roomId)

    log.debug { "Incoming notification, processing..." }

    runBlocking {
        val matrixMultiMessenger = runBlocking {
            multiMessengerHolder.getOrCreate {
                MatrixMultiMessenger.create {
                    messengerConfiguration {
                        modulesFactories += listOf { notificationsModule(this, false) }
                    }
                }
            }
        }

        val messenger = matrixMultiMessenger.activeMatrixMessenger.filterNotNull().first()
        log.debug { "Selected active messenger from multi messenger" }
        val matrixClients = messenger.di.get<MatrixClients>()
        matrixClients.isInitialized.first { it }

        val matrixClient = matrixClients.first { it.isNotEmpty() }[userId] ?: run {
            log.error { "MatrixClient not found for user $userId" }
            return@runBlocking
        }

        log.debug { "Found matrix client for user $userId" }
        val roomName = messenger.di.get<RoomName>().getRoomName(roomId, matrixClient).first()
        val notificationHandlerProvider = messenger.di.get<NotificationHandlerProvider>()
        matrixClient.syncOnce { sync ->
            log.debug { "Try to get notifications" }
            withTimeoutOrNull(15.seconds) {
                matrixClient.notification.getNotifications(sync.syncResponse).collect { notification ->
                    log.debug { "Received notification for event: ${notification.event.idOrNull}" }
                    val content = notification.event.content
                    val message = when {
                        content is MemberEventContent && content.membership == Membership.INVITE -> roomName
                        content is RoomMessageEventContent -> content.body
                        else -> null
                    }

                    if (message == null) {
                        log.error { "Unsupported notification content, ignoring event..." }
                        return@collect
                    }

                    val notificationHandler = notificationHandlerProvider(matrixClient.userId.toString())
                    notificationHandler.requestPermissions()
                    notificationHandler.push(
                        Notification(
                            title = roomName,
                            description = message,
                            callbackData = "$userId-${notification.event.roomIdOrNull}"
                        )
                    )
                }
            } ?: log.error { "Failed to receive notifications in timeout" }
        }.onFailure {
            log.error(it) { "Failed to receive notifications" }
        }
    }
}
