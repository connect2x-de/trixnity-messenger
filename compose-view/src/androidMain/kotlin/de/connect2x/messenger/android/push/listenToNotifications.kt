package de.connect2x.messenger.android.push

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toIcon
import de.connect2x.messenger.android.MessengerActivity
import de.connect2x.messenger.android.backgroundSyncShouldBeRunning
import de.connect2x.messenger.compose.view.R
import de.connect2x.messenger.compose.view.settings.pushChannelId
import de.connect2x.trixnity.messenger.MatrixMessenger
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.viewmodel.util.RoomName
import de.connect2x.trixnity.messenger.viewmodel.util.avatarSize
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.first
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.media
import net.folivo.trixnity.client.notification
import net.folivo.trixnity.client.notification.NotificationService
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.store.avatarUrl
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.ClientEvent
import net.folivo.trixnity.core.model.events.EventContent
import net.folivo.trixnity.core.model.events.RedactedEventContent
import net.folivo.trixnity.core.model.events.idOrNull
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership.INVITE
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.core.model.events.originTimestampOrNull
import net.folivo.trixnity.core.model.events.roomIdOrNull
import net.folivo.trixnity.core.model.events.senderOrNull
import net.folivo.trixnity.utils.toByteArray
import org.koin.core.Koin
import java.time.Instant

private val log = KotlinLogging.logger { }

suspend fun listenToNotifications(
    context: Context,
    matrixMessenger: MatrixMessenger,
    matrixClient: MatrixClient,
    expectedRoomId: RoomId,
    expectedEventId: EventId,
) {
    log.debug { "receive single notification" }
    val notification = matrixClient.notification.getNotifications().first {
        log.trace { "notification: ${it.event.idOrNull} in ${it.event.roomIdOrNull} from ${it.event.senderOrNull}" }
        expectedRoomId == it.event.roomIdOrNull && expectedEventId == it.event.idOrNull
    }
    log.debug { "received notification for event: ${notification.event.idOrNull}" }

    reactToNotification(context, matrixClient, matrixMessenger.di, notification)
}

private suspend fun reactToNotification(
    context: Context,
    matrixClient: MatrixClient,
    di: Koin,
    notification: NotificationService.Notification,
) {
    val roomNameComputation = di.get<RoomName>()
    // although the initial request could have only been started when the activity is not in RESUMED state, this
    // could have changed in the meantime (long lasting request, etc.), so check here again
    if (context.backgroundSyncShouldBeRunning) {
        val roomId = notification.event.roomIdOrNull

        log.debug { "create notification for UI" }
        val room = roomId?.let { matrixClient.room.getById(it).first() }
        val isDirect = room?.isDirect ?: false
        val roomName =
            room?.let {
                roomNameComputation.getRoomName(roomId, matrixClient).first()
            } ?: ""
        displayNotification(
            context,
            matrixClient,
            di.get(),
            notification.event,
            notification.event.content,
            isDirect,
            roomName,
        )
    }
}

private suspend fun displayNotification(
    context: Context,
    matrixClient: MatrixClient,
    matrixMessengerConfiguration: MatrixMessengerConfiguration,
    event: ClientEvent<*>, // possibly decrypted
    content: EventContent,
    isDirect: Boolean,
    roomName: String,
) {
    log.debug { "content is RoomMessageEventContent: ${content is RoomMessageEventContent}" }

    event.roomIdOrNull?.let { roomId ->
        if (content is RedactedEventContent) {
            log.debug { "redacted event: $content" }
            NotificationManagerCompat.from(context)
                .cancel(notificationId(roomId)) // TODO this removes ALL notifications, not only the redacted
        } else {
            val message = when {
                content is MemberEventContent && content.membership == INVITE -> roomName
                content is RoomMessageEventContent -> content.body
                else -> null
            }

            if (message != null) {
                val messenger =
                    Intent(context, MessengerActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                val startMessenger =
                    PendingIntent.getActivity(
                        context,
                        0,
                        messenger,
                        PendingIntent.FLAG_IMMUTABLE
                    )

                val (username, userImage) = event.senderOrNull?.let { sender ->
                    val user = matrixClient.user.getById(roomId, sender).first()
                    val image = user?.avatarUrl?.let { avatarUrl ->
                        matrixClient.media.getThumbnail(avatarUrl, avatarSize().toLong(), avatarSize().toLong())
                    }?.map { it.toByteArray() }
                        ?.map { bytes ->
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size).getCircledBitmap()
                        }?.getOrNull()
                    user?.name to image
                } ?: (null to null)

                val person =
                    Person.Builder().apply {
                        setName(username)
                        userImage?.let { setIcon(IconCompat.createFromIcon(context, it.toIcon())) }
                    }.build()

                val notificationMessage = NotificationCompat.MessagingStyle.Message(
                    message, event.originTimestampOrNull ?: Instant.now().toEpochMilli(), person
                )
                val messagingStyle =
                    restoreMessagingStyle(context, notificationId(roomId))
                        ?: NotificationCompat.MessagingStyle(person)
                messagingStyle.also {
                    it.addMessage(notificationMessage)
                    it.conversationTitle = if (isDirect) "" else roomName
                    it.isGroupConversation = isDirect.not()
                }

                val notification =
                    NotificationCompat.Builder(
                        context,
                        pushChannelId(matrixClient.userId, matrixMessengerConfiguration)
                    ).apply {
                        setStyle(messagingStyle)
                        setSmallIcon(R.drawable.ic_logo)
                        color = ContextCompat.getColor(context, R.color.logo)
                        setContentIntent(startMessenger)
                        setAutoCancel(true)
                        priority = NotificationCompat.PRIORITY_DEFAULT
                        setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                    }.build()

                log.debug { "notification will appear" }
                with(NotificationManagerCompat.from(context)) {
                    val notificationId = notificationId(roomId)
                    log.debug { "are notifications enabled: ${this.areNotificationsEnabled()}" }
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notify(notificationId, notification)
                    }
                }
            }
        }
    } ?: log.warn { "cannot get roomId for event ${event.idOrNull}" }
}

private fun restoreMessagingStyle(context: Context, notificationId: Int): NotificationCompat.MessagingStyle? {
    return (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        .activeNotifications
        .find { it.id == notificationId }
        ?.notification
        ?.let { NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(it) }
}

private fun notificationId(roomId: RoomId) = roomId.hashCode()


private fun Bitmap.getCircledBitmap(): Bitmap {
    val output = Bitmap.createBitmap(this.width, this.height, Bitmap.Config.ARGB_8888)
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
