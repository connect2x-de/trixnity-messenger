package de.connect2x.messenger.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.toAwtImage
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.IsFocused
import de.connect2x.messenger.compose.view.files.toImageBitmap
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.sysnotify.Notification
import de.connect2x.sysnotify.NotificationHandler
import de.connect2x.sysnotify.NotificationIcon
import de.connect2x.sysnotify.fromBufferedImage
import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.MatrixMessenger
import de.connect2x.trixnity.messenger.MatrixMessengerAccountSettings
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.platformNotifications
import de.connect2x.trixnity.messenger.viewmodel.util.RoomName
import de.connect2x.trixnity.messenger.viewmodel.util.avatarSize
import de.connect2x.trixnity.messenger.viewmodel.util.limitedByteArrayOrNull
import de.connect2x.trixnity.messenger.viewmodel.util.scopedCollectLatest
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.media
import net.folivo.trixnity.client.notification
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.store.avatarUrl
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.model.events.ClientEvent
import net.folivo.trixnity.core.model.events.EventContent
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
    notificationHandler: NotificationHandler
) {
    val i18n = DI.get<I18nView>()
    val maxAvatarSize = DI.get<MatrixMessengerConfiguration>().avatarMaxSize

    val windowIsFocused = IsFocused.current
    LaunchedEffect(windowIsFocused) {
        withContext(Dispatchers.Default) {
            val roomNameComputation = matrixMessenger.di.get<RoomName>()
            whenSyncIsRunning(
                matrixMessenger,
                windowIsFocused,
                roomNameComputation,
                i18n,
                maxAvatarSize,
                notificationHandler
            )
        }
    }
}

private suspend fun whenSyncIsRunning(
    matrixMessenger: MatrixMessenger,
    windowIsFocused: Boolean,
    roomNameComputation: RoomName,
    i18n: I18nView,
    maxAvatarSize: Long,
    notificationHandler: NotificationHandler
) {
    val settings = matrixMessenger.di.get<MatrixMessengerSettingsHolder>()
    matrixMessenger.di.get<MatrixClients>().scopedCollectLatest { matrixClients ->
        matrixClients.forEach { (userId, matrixClient) ->
            log.info { "notifications (whenSyncIsRunning): $userId" }
            launch {
                matrixClient.notification.getNotifications().collect { notification ->
                    val currentSettings = settings[userId].first() ?: return@collect
                    log.debug { "windowIsFocused: $windowIsFocused, currentSettings.base.notificationsEnabled: ${currentSettings.base.notificationsEnabled}" }
                    if (windowIsFocused.not() && currentSettings.base.notificationsEnabled) {
                        log.debug { "received notification for event ${notification.event.idOrNull}" }
                        if (currentSettings.platformNotifications.notificationsPlaySound &&
                            notification.actions.any { it is PushAction.SetSoundTweak }
                        ) {
                            // TODO: implement a sound flag in SysNotify 1.6.2
                        }
                        if (currentSettings.platformNotifications.notificationsShowPopup) {
                            val room = notification.event.roomIdOrNull?.let { matrixClient.room.getById(it).first() }
                            val isDirect = room?.isDirect ?: false
                            val roomName =
                                room?.let { roomNameComputation.getRoomName(room, matrixClient).first() } ?: ""
                            displayNotification(
                                currentSettings,
                                matrixClient,
                                notification.event,
                                notification.event.content,
                                isDirect,
                                roomName,
                                i18n,
                                maxAvatarSize,
                                notificationHandler
                            )
                        }
                    }
                }
            }
        }
    }
}

private suspend fun displayNotification(
    currentSettings: MatrixMessengerAccountSettings,
    matrixClient: MatrixClient,
    event: ClientEvent<*>,
    content: EventContent, // possibly decrypted
    isDirect: Boolean,
    roomName: String,
    i18n: I18nView,
    maxAvatarSize: Long,
    notificationHandler: NotificationHandler
) {
    event.roomIdOrNull?.let { roomId ->
        val message = when {
            currentSettings.platformNotifications.notificationsShowText.not() -> "(${i18n.newMessage()})"
            content is MemberEventContent && content.membership == Membership.INVITE -> roomName
            content is RoomMessageEventContent -> content.body
            else -> null
        }

        if (message != null) {
            val (username, image) = event.senderOrNull?.let { sender ->
                val user = matrixClient.user.getById(roomId, sender).first()
                val image = user?.avatarUrl?.let { avatarUrl ->
                    matrixClient.media.getThumbnail(avatarUrl, avatarSize().toLong(), avatarSize().toLong())
                }?.map { it.limitedByteArrayOrNull(maxAvatarSize) }
                    ?.map { bytes -> bytes?.toImageBitmap() }
                    ?.getOrNull()
                user?.name to image
            } ?: (null to null)

            log.debug { "notification will appear" }
            notificationHandler.push(
                Notification(
                    title = if (isDirect) username.orEmpty() else roomName,
                    group = "${matrixClient.userId}-$roomId",
                    description = if (isDirect) message else "$username: $message",
                    icon = image?.toAwtImage()?.let(NotificationIcon::fromBufferedImage)
                )
            )
        }
    } ?: log.warn { "cannot find roomId for event ${event.idOrNull}" }
}
