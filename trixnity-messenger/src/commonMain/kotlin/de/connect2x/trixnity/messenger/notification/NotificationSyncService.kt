package de.connect2x.trixnity.messenger.notification

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.lognity.api.logger.error
import de.connect2x.sysnotify.Notification
import de.connect2x.sysnotify.NotificationHandler
import de.connect2x.sysnotify.NotificationIcon
import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.media
import de.connect2x.trixnity.client.notification
import de.connect2x.trixnity.client.notification.NotificationUpdate
import de.connect2x.trixnity.client.store.avatarUrl
import de.connect2x.trixnity.client.store.roomId
import de.connect2x.trixnity.client.store.sender
import de.connect2x.trixnity.client.user
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.m.room.MemberEventContent
import de.connect2x.trixnity.core.model.events.m.room.Membership
import de.connect2x.trixnity.core.model.events.m.room.RoomMessageEventContent
import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.MatrixMessengerAccountNotificationSettings
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.Worker
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.viewmodel.util.RoomName
import de.connect2x.trixnity.messenger.viewmodel.util.avatarSize
import de.connect2x.trixnity.messenger.viewmodel.util.scopedCollectLatest
import de.connect2x.trixnity.utils.toByteArray
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class NotificationSyncService(
    private val matrixClients: MatrixClients,
    private val notificationHandlers: NotificationHandlers,
    private val notificationProviders: NotificationProviders,
    private val config: MatrixMessengerConfiguration,
    private val settings: MatrixMessengerSettingsHolder,
    private val roomName: RoomName,
    private val getNotificationIcon: GetNotificationIcon?,
    private val i18n: I18n,
) : Worker {
    companion object {
        private val log: Logger = Logger("de.connect2x.trixnity.messenger.notification.NotificationService")
        internal const val noDetailsTag = "NO_DETAILS_PLACEHOLDER"
    }

    private val statusIcon: NotificationIcon? by lazy {
        val path = config.appIcon
        if (path == null || getNotificationIcon == null) return@lazy null
        getNotificationIcon.fromResource(path)
    }

    override suspend fun doWork() {
        syncNotifications()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun syncNotifications() {
        data class SyncNotificationsForAccountData(
            val account: UserId,
            val matrixClient: MatrixClient,
            val notificationsEnabled: Boolean,
            val notificationSettings: MatrixMessengerAccountNotificationSettings,
        )
        matrixClients
            .flatMapLatest { matrixClients ->
                combine(
                    matrixClients.map { (userId, matrixClient) ->
                        combine(
                            combine(notificationProviders.map { it.isEnabled(userId) }) { it.any { it } },
                            settings[userId].filterNotNull().map { it.notification },
                        ) { notificationsEnabled, notificationSettings ->
                            SyncNotificationsForAccountData(
                                account = userId,
                                matrixClient = matrixClient,
                                notificationsEnabled = notificationsEnabled,
                                notificationSettings = notificationSettings,
                            )
                        }
                    }
                ) {
                    it.toList()
                }
            }
            .scopedCollectLatest { syncNotificationsForAccountData ->
                syncNotificationsForAccountData.forEach { syncNotificationsForAccount ->
                    launch {
                        syncNotificationsForAccount(
                            notificationSettings = syncNotificationsForAccount.notificationSettings,
                            notificationHandler = notificationHandlers[syncNotificationsForAccount.account],
                            matrixClient = syncNotificationsForAccount.matrixClient,
                            notificationsEnabled = syncNotificationsForAccount.notificationsEnabled,
                        )
                    }
                }
            }
    }

    private suspend fun syncNotificationsForAccount(
        notificationsEnabled: Boolean,
        notificationSettings: MatrixMessengerAccountNotificationSettings,
        notificationHandler: NotificationHandler,
        matrixClient: MatrixClient,
    ) {
        if (!notificationsEnabled) {
            log.debug { "clear all notifications for ${matrixClient.userId}, because notifications disabled" }
            /* FIXME discuss if this call is necessary and fix when notificationHandler.clearAll() is fixed for all platforms
            try {
                notificationHandler.clearAll()
            } catch (e: Throwable) {
                log.error(e) { "failed to clear all notifications" }
            }
            */
            matrixClient.notification.getAllUpdates().collect() // black hole
            return
        }

        coroutineScope {
            launch {
                matrixClient.notification.getCount().distinctUntilChanged().collect { count ->
                    if (!notificationSettings.showDetails && count == 0) {
                        executeAction("remove", noDetailsTag) { notificationHandler.pop(noDetailsTag) }
                    }
                }
            }

            log.debug { "listen for new notifications for ${matrixClient.userId}" }
            matrixClient.notification.getAllUpdates().collect { notificationUpdate ->
                notificationUpdate.send(
                    showDetails = notificationSettings.showDetails,
                    playSound = notificationSettings.playSound,
                    notificationHandler = notificationHandler,
                    matrixClient = matrixClient,
                )
            }
        }
    }

    private suspend fun NotificationUpdate.send(
        showDetails: Boolean,
        playSound: Boolean,
        notificationHandler: NotificationHandler,
        matrixClient: MatrixClient,
    ) {
        val tag = if (showDetails) id else noDetailsTag

        suspend fun resolveNotification(
            playSound: Boolean,
            extractData: suspend () -> NotificationData?,
        ): Notification? {
            return if (showDetails) {
                val data = extractData() ?: return null
                buildDetailedNotification(data, playSound)
            } else {
                buildAnonymousNotification(playSound)
            }
        }

        when (this) {
            is NotificationUpdate.New -> {
                val notification = resolveNotification(playSound) { content.toNotificationData(matrixClient) } ?: return
                executeAction("push", tag) { notificationHandler.push(notification, tag) }
            }
            is NotificationUpdate.Update -> {
                if (showDetails) {
                    val notification = resolveNotification(false) { content.toNotificationData(matrixClient) } ?: return
                    executeAction("update", tag) { notificationHandler.update(tag, notification) }
                } else {
                    val notification =
                        resolveNotification(playSound) { content.toNotificationData(matrixClient) } ?: return
                    executeAction("push", tag) { notificationHandler.push(notification, tag) }
                }
            }
            is NotificationUpdate.Remove -> {
                if (!showDetails) return // handled by coroutine scope
                executeAction("remove", tag) { notificationHandler.pop(tag) }
            }
        }
    }

    private fun buildDetailedNotification(data: NotificationData, playSound: Boolean): Notification {
        return Notification(
            title = data.title,
            description = data.description,
            icon = data.icon,
            statusIcon = statusIcon,
            callbackData = data.callbackData,
            playSound = playSound,
        )
    }

    private fun buildAnonymousNotification(playSound: Boolean): Notification {
        return Notification(
            title = i18n.newMessageTitle(),
            description = i18n.newMessageDescription(),
            callbackData = getCallback(),
            playSound = playSound,
        )
    }

    private inline fun executeAction(action: String, tag: String, block: () -> Unit) {
        log.debug { "$action notification in system (tag=$tag)" }

        try {
            block()
        } catch (e: Throwable) {
            log.error(e) { "failed to $action notification (tag=$tag)" }
        }
    }

    private data class NotificationData(
        val title: String,
        val description: String?,
        val icon: NotificationIcon?,
        val callbackData: String?,
    )

    private suspend fun NotificationUpdate.Content.toNotificationData(matrixClient: MatrixClient): NotificationData? {
        val title: String
        val description: String?
        val senderAvatar: String?
        val roomId: RoomId?
        when (this) {
            is NotificationUpdate.Content.Message -> {
                val timelineEventContent = timelineEvent.content?.getOrNull() ?: timelineEvent.event.content
                val messageBody = (timelineEventContent as? RoomMessageEventContent)?.body
                if (messageBody == null) {
                    log.debug {
                        "notification message content ${timelineEventContent::class.simpleName} is not supported"
                    }
                    return null
                }
                val sender = matrixClient.user.getById(timelineEvent.roomId, timelineEvent.sender).first()
                val senderName = sender?.name ?: timelineEvent.sender.full

                senderAvatar = sender?.avatarUrl?.takeIf { getNotificationIcon != null }
                title = roomName.getRoomName(timelineEvent.roomId, matrixClient).first()
                description = "$senderName: $messageBody"
                roomId = timelineEvent.roomId
            }

            is NotificationUpdate.Content.State -> {
                val stateEventContent = stateEvent.content
                val roomName = stateEvent.roomId?.let { roomName.getRoomName(it, matrixClient) }?.first()
                when (stateEventContent) {
                    is MemberEventContent if
                        stateEventContent.membership == Membership.INVITE &&
                            stateEvent.stateKey == matrixClient.userId.full
                     -> {
                        title = roomName ?: i18n.newInvite()
                        description = if (roomName != null) i18n.newInvite() else null
                    }

                    else -> {
                        title = roomName ?: i18n.newActivity()
                        description = if (roomName != null) i18n.newActivity() else null
                    }
                }
                senderAvatar =
                    stateEvent.roomId
                        .takeIf { getNotificationIcon != null }
                        ?.let { roomId -> matrixClient.user.getById(roomId, stateEvent.sender).first()?.avatarUrl }
                roomId = stateEvent.roomId
            }
        }
        val icon =
            senderAvatar
                ?.let {
                    withTimeoutOrNull(300.milliseconds) {
                        matrixClient.media
                            .getThumbnail(it, avatarSize().toLong(), avatarSize().toLong())
                            .getOrNull()
                            ?.toByteArray(config.maxMediaSizeInMemory)
                    }
                }
                ?.let { getNotificationIcon?.fromBytes(it, avatarSize(), avatarSize()) }
        val callbackData = if (roomId != null) getCallback(roomId) else null
        return NotificationData(title = title, description = description, icon = icon, callbackData = callbackData)
    }

    private fun getCallback(roomId: RoomId? = null): String {
        return buildString { // TODO does this need to be changed in JS?
            append("${config.appUri}/")
            roomId?.let { append("matrix:roomid/${roomId.full.trimStart(RoomId.sigilCharacter)}") }
        }
    }
}
