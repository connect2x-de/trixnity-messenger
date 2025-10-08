package de.connect2x.trixnity.messenger.notification

import de.connect2x.sysnotify.Notification
import de.connect2x.sysnotify.NotificationHandler
import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.MatrixMessengerAccountNotificationSettings
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.Worker
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.viewmodel.util.RoomName
import de.connect2x.trixnity.messenger.viewmodel.util.scopedCollectLatest
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.notification
import net.folivo.trixnity.client.notification.NotificationUpdate
import net.folivo.trixnity.client.store.roomId
import net.folivo.trixnity.client.store.sender
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent

private val log = KotlinLogging.logger("de.connect2x.trixnity.messenger.notification.NotificationService")

class NotificationSyncService(
    private val matrixClients: MatrixClients,
    private val notificationHandlers: NotificationHandlers,
    private val notificationProviders: NotificationProviders,
    private val settings: MatrixMessengerSettingsHolder,
    private val roomName: RoomName,
    private val i18n: I18n,
) : Worker {

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
        matrixClients.flatMapLatest { matrixClients ->
            combine(matrixClients.map { (userId, matrixClient) ->
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
            }) {
                it.toList()
            }
        }.scopedCollectLatest { syncNotificationsForAccountDatas ->
            syncNotificationsForAccountDatas.forEach { syncNotificationsForAccount ->
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
        if (notificationsEnabled) {
            log.debug { "listen for new notifications for ${matrixClient.userId}" }
            matrixClient.notification.getAllUpdates()
                .collect { notificationUpdate ->
                    if (notificationSettings.showDetails) {
                        notificationUpdate.send(
                            playSound = notificationSettings.playSound,
                            notificationHandler = notificationHandler,
                            matrixClient = matrixClient
                        )
                    } else {
                        notificationHandler.push(
                            tag = "NO_DETAILS_PLACEHOLDER",
                            notification = Notification(
                                title = i18n.newMessageTitle(),
                                description = i18n.newMessageDescription(),
                                playSound = notificationSettings.playSound,
                            ),
                        )
                    }
                }
        } else {
            log.debug { "clear all notifications for ${matrixClient.userId}, because notifications disabled" }
            notificationHandler.clearAll()
            matrixClient.notification.getAllUpdates().collect() // black hole
        }
    }

    private suspend fun NotificationUpdate.send(
        playSound: Boolean,
        notificationHandler: NotificationHandler,
        matrixClient: MatrixClient,
    ) {
        when (this) {
            is NotificationUpdate.New -> {
                val notificationData = content.toNotificationData(matrixClient)
                notificationHandler.push(
                    tag = id,
                    notification = Notification(
                        title = notificationData.title,
                        description = notificationData.description,
                        callbackData = notificationData.callbackData,
                        playSound = playSound,
                    )
                )
            }

            is NotificationUpdate.Update -> {
                val notificationData = content.toNotificationData(matrixClient)
                notificationHandler.update(
                    tag = id,
                    notification = Notification(
                        title = notificationData.title,
                        description = notificationData.description,
                        callbackData = notificationData.callbackData,
                        playSound = false,
                    )
                )
            }

            is NotificationUpdate.Remove -> {
                notificationHandler.pop(tag = id)
            }
        }
    }

    private data class NotificationData(
        val title: String,
        val description: String?,
        val callbackData: String?,
    )

    private suspend fun NotificationUpdate.Content.toNotificationData(matrixClient: MatrixClient): NotificationData {
        val title: String
        val description: String?
        val roomId: RoomId?
        when (this) {
            is NotificationUpdate.Content.Message -> {
                val sender = matrixClient.user.getById(timelineEvent.roomId, timelineEvent.sender).first()?.name
                    ?: timelineEvent.sender.full
                title = roomName.getRoomName(timelineEvent.roomId, matrixClient).first()
                description = "$sender: " + ((timelineEvent.content?.getOrNull() as? RoomMessageEventContent)?.body
                    ?: i18n.commonUnknown())
                roomId = timelineEvent.roomId
            }

            is NotificationUpdate.Content.State -> {
                title =
                    stateEvent.roomId?.let { roomName.getRoomName(it, matrixClient) }?.first() ?: i18n.newMessageTitle()
                description = null
                roomId = stateEvent.roomId
            }
        }
        val callbackData =
            if (roomId != null) buildString {
                append("matrix:roomid/")
                append(roomId.full.trimStart(RoomId.sigilCharacter))
            }
            else null
        return NotificationData(
            title = title,
            description = description,
            callbackData = callbackData,
        )
    }
}
