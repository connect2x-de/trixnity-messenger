package de.connect2x.messenger.compose.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.sysnotify.Notification
import de.connect2x.sysnotify.NotificationHandler
import de.connect2x.sysnotify.NotificationIcon
import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.MatrixMessenger
import de.connect2x.trixnity.messenger.MatrixMessengerAccountSettings
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.util.currentImmediateDispatcher
import de.connect2x.trixnity.messenger.viewmodel.util.RoomName
import de.connect2x.trixnity.messenger.viewmodel.util.avatarSize
import de.connect2x.trixnity.messenger.viewmodel.util.scopedCollectLatest
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.media
import net.folivo.trixnity.client.notification
import net.folivo.trixnity.client.notification.NotificationService
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.store.avatarUrl
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.model.events.idOrNull
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.core.model.events.roomIdOrNull
import net.folivo.trixnity.core.model.events.senderOrNull
import net.folivo.trixnity.core.model.push.PushAction

private val log = KotlinLogging.logger { }

@Composable
fun Notifications(
    matrixMessenger: MatrixMessenger,
) {
    val i18n = DI.get<I18nView>()
    val config = matrixMessenger.di.get<MatrixMessengerConfiguration>()
    val notificationHandler = NotificationHandler(
        name = "${config.appName} Notifications",
        id = "${config.appId}.notification",
        isDebugEnabled = config.notificationsDebugEnabled,
    )

    LaunchedEffect(Unit) {
        matrixMessenger.di.get<MatrixMessengerSettingsHolder>()
            .map { it.base.accounts }
            .distinctUntilChanged()
            .conflate()
            .collect { settings ->
                val anyNotificationsEnabled =
                    settings.any { (_, settings) -> settings.base.notificationsEnabled }
                withContext(currentImmediateDispatcher()) {
                    if (anyNotificationsEnabled) {
                        log.debug { "Notifications are enabled for active messenger, requesting permissions" }
                        notificationHandler.requestPermissions()
                    }
                }
            }
    }

    val windowIsFocused = IsFocused.current
    LaunchedEffect(windowIsFocused) {
        withContext(Dispatchers.Default) {
            log.debug { "window is focused: $windowIsFocused" }
            val roomNameComputation = matrixMessenger.di.get<RoomName>()
            whenSyncIsRunning(matrixMessenger, windowIsFocused, roomNameComputation, notificationHandler, i18n)
        }
    }
}

private suspend fun whenSyncIsRunning(
    matrixMessenger: MatrixMessenger,
    windowIsFocused: Boolean,
    roomNameComputation: RoomName,
    notificationHandler: NotificationHandler,
    i18n: I18nView
) {
    val settings = matrixMessenger.di.get<MatrixMessengerSettingsHolder>()
    val maxMediaSizeInMemory = matrixMessenger.di.get<MatrixMessengerConfiguration>().maxMediaSizeInMemory
    matrixMessenger.di.get<MatrixClients>().scopedCollectLatest { matrixClients ->
        matrixClients.forEach { (userId, matrixClient) ->
            launch {
                matrixClient.notification.getNotifications().collect { notification ->
                    val currentSettings = settings[userId].first() ?: return@collect
                    log.debug { "windowIsFocused: $windowIsFocused" }
                    if (windowIsFocused.not() && currentSettings.base.notificationsEnabled) {
                        log.debug { "received notification for event ${notification.event.idOrNull}" }
                        // sound from web?
                        if (shouldShowPopup(currentSettings)) {
                            val room = notification.event.roomIdOrNull?.let { matrixClient.room.getById(it).first() }
                            val isDirect = room?.isDirect ?: false
                            val roomName =
                                room?.let { roomNameComputation.getRoomName(room, matrixClient).first() } ?: ""
                            displayNotification(
                                matrixClient,
                                notification,
                                currentSettings,
                                isDirect,
                                roomName,
                                i18n,
                                maxMediaSizeInMemory
                            )?.let {
                                notificationHandler.push(it)
                            }
                        }
                    }
                }
            }
        }
    }
}

private suspend fun displayNotification(
    matrixClient: MatrixClient,
    notification: NotificationService.Notification,
    currentSettings: MatrixMessengerAccountSettings,
    isDirect: Boolean,
    roomName: String,
    i18n: I18nView,
    maxMediaSizeInMemory: Long
): Notification? {
    val event = notification.event
    val content = event.content // possibly encrypted
    event.roomIdOrNull?.let { roomId ->
        val message = when {
            shouldShowText(currentSettings).not() -> "(${i18n.newMessage()})"
            content is MemberEventContent && content.membership == Membership.INVITE -> roomName
            content is RoomMessageEventContent -> content.body
            else -> null
        }

        if (message != null) {
            val (username, imageInBytes) = event.senderOrNull?.let { sender ->
                val user = matrixClient.user.getById(roomId, sender).first()
                val image = user?.avatarUrl?.let { avatarUrl ->
                    matrixClient.media.getThumbnail(avatarUrl, avatarSize().toLong(), avatarSize().toLong())
                }?.map { it.toByteArray(maxSize = maxMediaSizeInMemory) }?.getOrNull()
                user?.name to image
            } ?: (null to null)

            val title = if (isDirect) username.orEmpty() else roomName
            val text = if (isDirect) message else "$username: $message"

            log.debug { "notification will appear" }

            return Notification(
                title = title,
                description = text,
                icon = imageInBytes?.let { NotificationIcon(it, avatarSize(), avatarSize()) },
                playSound = shouldPlaySound(currentSettings) && notification.actions.any { it is PushAction.SetSoundTweak }
            )
        }
    } ?: log.warn { "cannot find roomId for event ${event.idOrNull}" }
    return null
}

expect fun shouldShowPopup(currentSettings: MatrixMessengerAccountSettings): Boolean
expect fun shouldShowText(currentSettings: MatrixMessengerAccountSettings): Boolean
expect fun shouldPlaySound(currentSettings: MatrixMessengerAccountSettings): Boolean
