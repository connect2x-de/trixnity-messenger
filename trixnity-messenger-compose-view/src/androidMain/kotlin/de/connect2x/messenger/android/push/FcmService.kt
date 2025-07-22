package de.connect2x.messenger.android.push

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import de.connect2x.messenger.android.backgroundSyncShouldBeRunning
import de.connect2x.messenger.android.withMatrixMessengerService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import java.time.Duration

class FcmService : FirebaseMessagingService() {
    private val log = KotlinLogging.logger { }

    override fun onCreate() {
        log.debug { "Creating FcmService" }
        super.onCreate()
    }

    override fun onDestroy() {
        log.debug { "Destroying FcmService" }
        super.onDestroy()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        log.debug { "Received FCM message" }
        val shouldBeRunning = baseContext.backgroundSyncShouldBeRunning
        if (shouldBeRunning) {
            val roomId = remoteMessage.data["room_id"].orEmpty()
            val eventId = remoteMessage.data["event_id"].orEmpty()
            log.debug { "Received push message (from=${remoteMessage.from}, roomId=$roomId, eventId=$eventId" }

            if (roomId.isNotEmpty() && eventId.isNotEmpty()) {
                val workRequest = OneTimeWorkRequestBuilder<FcmNotificationsWorker>()
                    .setInputData(
                        workDataOf(
                            "roomId" to roomId,
                            "eventId" to eventId,
                        )
                    )
                    .setBackoffCriteria(BackoffPolicy.LINEAR, Duration.ZERO)
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .build()
                WorkManager.getInstance(applicationContext).enqueue(workRequest)
            }
        }
    }

    override fun onNewToken(token: String) {
        log.info { "Got new FCM token" }
        runBlocking {
            withMatrixMessengerService(applicationContext) { matrixMultiMessenger ->
                val matrixMessenger = matrixMultiMessenger.activeMatrixMessenger.value
                    ?: return@withMatrixMessengerService
                setMatrixPushers(matrixMessenger, token, this)
            }
        }
    }
}
