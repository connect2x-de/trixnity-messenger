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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.Duration

private val log = KotlinLogging.logger { }

class FcmService : FirebaseMessagingService() {
    private val scope = CoroutineScope(Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        log.debug { "init FcmPushService" }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        log.debug { "on message received" }
        val shouldBeRunning = baseContext.backgroundSyncShouldBeRunning
        log.debug { "backgroundSyncShouldBeRunning: $shouldBeRunning" }

        if (shouldBeRunning) {
            val roomId = remoteMessage.data["room_id"].orEmpty()
            val eventId = remoteMessage.data["event_id"].orEmpty()
            log.debug { "received push message (from=${remoteMessage.from}, roomId=$roomId, eventId=$eventId" }

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
        log.info { "new FCM token" }
        scope.launch {
            withMatrixMessengerService(applicationContext) { matrixMultiMessenger ->
                val matrixMessenger = matrixMultiMessenger.activeMatrixMessenger.value
                    ?: return@withMatrixMessengerService
                setPushersForMatrixClientsWithPush(matrixMessenger, token)
            }
        }
    }
}
