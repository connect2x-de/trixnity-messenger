package de.connect2x.messenger.android.push

import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toIcon
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import de.connect2x.messenger.android.NotificationHandlerProvider
import de.connect2x.messenger.android.backgroundSyncShouldBeRunning
import de.connect2x.messenger.android.withMatrixMessengerService
import de.connect2x.sysnotify.Notification
import de.connect2x.sysnotify.getNotificationIcon
import de.connect2x.sysnotify.push
import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.MatrixMessenger
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.PushMode
import de.connect2x.trixnity.messenger.compose.view.R
import de.connect2x.trixnity.messenger.platformNotifications
import de.connect2x.trixnity.messenger.viewmodel.util.RoomName
import de.connect2x.trixnity.messenger.viewmodel.util.avatarSize
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.media
import net.folivo.trixnity.client.notification
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.store.avatarUrl
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.ClientEvent
import net.folivo.trixnity.core.model.events.idOrNull
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership.INVITE
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.core.model.events.originTimestampOrNull
import net.folivo.trixnity.core.model.events.roomIdOrNull
import net.folivo.trixnity.core.model.events.senderOrNull
import java.time.Instant
import kotlin.time.Duration.Companion.seconds
import androidx.core.graphics.createBitmap

class FcmNotificationsWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    private val log = KotlinLogging.logger { }

    init {
        log.debug { "Initializing FcmNotificationsWorker" }
    }

    private suspend fun queryNotifications(
        context: Context,
        matrixMessenger: MatrixMessenger,
        matrixClient: MatrixClient,
        expectedRoomId: RoomId,
        expectedEventId: EventId,
        notificationHandlerProvider: NotificationHandlerProvider,
        maxAvatarSize: Long
    ) {
        val notification = matrixClient.notification.getNotifications().first {
            log.trace { "Notification: ${it.event.idOrNull} in ${it.event.roomIdOrNull} from ${it.event.senderOrNull}" }
            expectedRoomId == it.event.roomIdOrNull && expectedEventId == it.event.idOrNull
        }
        log.debug { "Received notification for event: ${notification.event.idOrNull}" }

        val roomNameComputation = matrixMessenger.di.get<RoomName>()
        // although the initial request could have only been started when the activity is not in RESUMED state, this
        // could have changed in the meantime (long-lasting request, etc.), so check here again
        if (context.backgroundSyncShouldBeRunning) {
            val roomId = notification.event.roomIdOrNull

            log.debug { "Create notification for UI" }
            val room = roomId?.let { matrixClient.room.getById(it).first() }
            val isDirect = room?.isDirect ?: false
            val roomName =
                room?.let {
                    roomNameComputation.getRoomName(roomId, matrixClient).first()
                } ?: ""
            displayNotification(
                context,
                matrixClient,
                notification.event,
                isDirect,
                roomName,
                notificationHandlerProvider,
                maxAvatarSize
            )
        }
    }

    private suspend fun displayNotification(
        context: Context,
        matrixClient: MatrixClient,
        event: ClientEvent<*>, // possibly decrypted
        isDirect: Boolean,
        roomName: String,
        notificationHandlerProvider: NotificationHandlerProvider,
        maxMediaSizeInMemory: Long
    ) {
        val content = event.content
        log.debug { "Content is RoomMessageEventContent: ${content is RoomMessageEventContent}" }

        event.roomIdOrNull?.let { roomId ->
            val message = when {
                content is MemberEventContent && content.membership == INVITE -> roomName
                content is RoomMessageEventContent -> content.body
                else -> null
            }

            if (message != null) {
                val sender = event.senderOrNull ?: return
                val user = sender.let { matrixClient.user.getById(roomId, it).first() } ?: return

                val (userName, userImage) = user.let {
                    val image = it.avatarUrl?.let { avatarUrl ->
                        matrixClient.media.getThumbnail(avatarUrl, avatarSize().toLong(), avatarSize().toLong())
                    }?.map { flow ->
                        val bytes = flow.toByteArray(maxSize = maxMediaSizeInMemory)
                        bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size).getCircledBitmap() }
                    }?.getOrNull()
                    user.name to image
                }

                notificationHandlerProvider.value[matrixClient.userId]?.push(
                    Notification(
                        title = roomName,
                        icon = context.resources.getNotificationIcon(R.drawable.ic_logo),
                        callbackData = "${matrixClient.userId}-$roomId"
                    )
                ) {
                    var style = restoreMessagingStyle(context, roomId)
                    if (style == null) {
                        val person = Person.Builder().apply {
                            setName(userName)
                            userImage?.let { setIcon(IconCompat.createFromIcon(context, it.toIcon())) }
                        }.build()
                        style = NotificationCompat.MessagingStyle(person).also {
                            it.addMessage(
                                NotificationCompat.MessagingStyle.Message(
                                    message, event.originTimestampOrNull ?: Instant.now().toEpochMilli(), person
                                )
                            )
                            it.conversationTitle = if (isDirect) "" else roomName
                            it.isGroupConversation = isDirect.not()
                        }
                    }
                    setStyle(style)
                    setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                    color = ContextCompat.getColor(context, R.color.logo)
                }
            }
        } ?: log.warn { "Cannot get roomId for event ${event.idOrNull}" }
    }

    private fun restoreMessagingStyle(context: Context, roomId: RoomId): NotificationCompat.MessagingStyle? {
        return (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .activeNotifications
            .find { it.id == roomId.hashCode() }
            ?.notification
            ?.let { NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(it) }
    }

    private fun Bitmap.getCircledBitmap(): Bitmap {
        val output = createBitmap(this.width, this.height)
        val canvas = Canvas(output)
        val paint = Paint()
        val rect = Rect(0, 0, this.width, this.height)
        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawCircle(this.width / 2f, this.height / 2f, this.width / 2f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(this, rect, rect, paint)
        return output
    }

    override suspend fun doWork(): Result {
        log.debug { "FcmNotificationsWorker.doWork" }
        return withMatrixMessengerService(applicationContext) { matrixMultiMessenger ->
            val matrixMessenger = matrixMultiMessenger.activeMatrixMessenger.value
                ?: return@withMatrixMessengerService Result.failure()
            coroutineScope {
                // we cannot assume that we should still be running
                val pushModes =
                    matrixMessenger.di.get<MatrixMessengerSettingsHolder>().value.base.accounts.map { it.value.platformNotifications.pushMode }
                val maxMediaSizeInMemory = matrixMessenger.di.get<MatrixMessengerConfiguration>().maxMediaSizeInMemory
                if (pushModes.none { it == PushMode.PUSH } ||
                    applicationContext.backgroundSyncShouldBeRunning.not()
                ) {
                    return@coroutineScope Result.failure()
                }

                val roomId = inputData.getString("roomId")?.let(::RoomId)
                val eventId = inputData.getString("eventId")?.let(::EventId)
                if (roomId == null || eventId == null) return@coroutineScope Result.failure()

                log.debug { "Got event $eventId in room $roomId" }

                findTheCorrespondingMatrixClient(
                    matrixMessenger,
                    roomId,
                )?.let { matrixClient ->
                    val listen = async {
                        queryNotifications(
                            applicationContext,
                            matrixMessenger,
                            matrixClient,
                            roomId,
                            eventId,
                            matrixMultiMessenger.di.get(),
                            maxMediaSizeInMemory
                        )
                    }

                    val result = matrixClient.syncOnce {}
                    result.onFailure {
                        log.error(it) { "Cannot get the event $eventId in room $roomId" }
                        listen.cancel()
                        return@coroutineScope Result.retry()
                    }
                    val listenResult = withTimeoutOrNull(5.seconds) {
                        listen.await()
                    }
                    if (listenResult == null) {
                        log.warn { "Received no notification for event $eventId in room $roomId" }
                    }

                    result.onSuccess {
                        log.debug { "Receive push event $eventId in room $roomId was successful -> end worker" }
                        return@coroutineScope Result.success()
                    }
                }
                return@coroutineScope Result.failure()
            }
        }
    }

    // since the account name is not known beforehand, we have to retrieve it here by checking which MatrixClient the
    // roomId belongs to
    // TODO this does not find rooms from invites (requires a sync)
    private suspend fun findTheCorrespondingMatrixClient(
        matrixMessenger: MatrixMessenger,
        roomId: RoomId,
    ): MatrixClient? {
        val matrixClient =
            matrixMessenger.di.get<MatrixClients>().value.values.firstOrNull { matrixClient ->
                matrixClient.room.getById(roomId).first() != null
            }

        if (matrixClient == null) {
            log.warn { "Cannot find a MatrixClient for the room $roomId" }
        }
        return matrixClient
    }
}
