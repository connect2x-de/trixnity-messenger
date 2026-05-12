package de.connect2x.trixnity.messenger.notification.fcm

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import de.connect2x.lognity.api.logger.Logger

private val log = Logger("de.connect2x.trixnity.messenger.notification.fcm")

class TrixnityMessengerFirebaseMessagingService : FirebaseMessagingService() {

    override fun onCreate() {
        log.trace { "onCreate" }
        super.onCreate()
    }

    override fun onDestroy() {
        log.trace { "onDestroy" }
        super.onDestroy()
    }

    override fun onNewToken(token: String) {
        log.trace { "onNewToken" }
        OnNewTokenWorker.enqueueUniqueWork(
            context = applicationContext, pushKey = token
        )
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        log.trace { "onMessageReceived (data=${remoteMessage.data})" }
        val profile = remoteMessage.data["profile"]
        val account = remoteMessage.data["account"]
        val roomId = remoteMessage.data["room_id"]
        val eventId = remoteMessage.data["event_id"]

        if (roomId != null) {
            OnMessageReceivedWorker.enqueueWork(
                context = applicationContext,
                profile = profile,
                account = account,
                roomId = roomId,
                eventId = eventId
            )
        }
    }
}
