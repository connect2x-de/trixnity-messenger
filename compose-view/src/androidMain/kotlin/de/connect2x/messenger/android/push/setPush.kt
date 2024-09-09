package de.connect2x.messenger.android.push

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.work.WorkManager
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import de.connect2x.messenger.android.CHANNEL_ID_RECEIVING
import de.connect2x.messenger.android.WORKER_BACKGROUND_SYNC
import de.connect2x.messenger.compose.view.R
import de.connect2x.messenger.compose.view.settings.pushChannelId
import de.connect2x.trixnity.messenger.MatrixMessenger
import de.connect2x.trixnity.messenger.PushMode
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import net.folivo.trixnity.core.model.UserId


private val log = KotlinLogging.logger { }

suspend fun setPush(
    context: Context,
    pushModesMapping: Map<UserId, PushMode>,
    matrixMessenger: MatrixMessenger,
) = coroutineScope {
    val pushModes = pushModesMapping.values
    log.debug { "push modes: $pushModes" }

    if (pushModes.contains(PushMode.PUSH)) {
        context.packageManager.setComponentEnabledSetting(
            ComponentName(context, FcmService::class.java),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP,
        )
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            if (token != null) {
                log.debug { "got FCM token" }
                setPushersForMatrixClientsWithPush(matrixMessenger, token)
            } else {
                log.warn { "FCM token is 'null'" }
            }
        } catch (exception: Exception) {
            log.error(exception) { "cannot get FCM token" }
        }
    } else {
        log.info { "since there are no accounts that have a push service enabled, disable the complete service" }
        context.packageManager.setComponentEnabledSetting(
            ComponentName(context, FcmService::class.java),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
        )
    }

    if (pushModes.contains(PushMode.POLLING)) {
        PollingNotificationsWorker.requestStart(context)
    } else {
        log.info { "since no accounts with polling policy are active, cancel POLLING worker" }
        WorkManager.getInstance(context).cancelUniqueWork(WORKER_BACKGROUND_SYNC)

        // we still need to cancel the notification as the WorkManager does not do that
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.activeNotifications.find {
            it.notification.channelId == CHANNEL_ID_RECEIVING
        }?.id?.let { notificationId ->
            log.debug { "cancel notification" }
            notificationManager.cancel(notificationId)
        }
    }

    pushModesMapping.forEach { (userId, _) ->
        log.info { "create notification channel for $userId" }
        val descriptionText = context.getString(R.string.push_channel_description)
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel =
            NotificationChannel(pushChannelId(userId, matrixMessenger.di.get()), userId.full, importance).apply {
                description = descriptionText
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
                lightColor = ContextCompat.getColor(context, R.color.logo)
                enableLights(true)
            }
        // Register the channel with the system
        val notificationManager: NotificationManager =
            context.getSystemService(FirebaseMessagingService.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}
