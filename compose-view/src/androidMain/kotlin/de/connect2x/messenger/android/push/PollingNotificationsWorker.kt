package de.connect2x.messenger.android.push

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import de.connect2x.messenger.android.CHANNEL_ID_RECEIVING
import de.connect2x.messenger.android.WORKER_BACKGROUND_SYNC
import de.connect2x.messenger.android.backgroundSyncShouldBeRunning
import de.connect2x.messenger.android.withMatrixMessengerService
import de.connect2x.messenger.compose.view.R
import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.PushMode
import de.connect2x.trixnity.messenger.platformNotifications
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.folivo.trixnity.clientserverapi.client.SyncState
import java.time.Duration
import kotlin.time.Duration.Companion.minutes


private val log = KotlinLogging.logger { }

class PollingNotificationsWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    companion object {
        fun requestStart(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<PollingNotificationsWorker>()
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, Duration.ZERO)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    WORKER_BACKGROUND_SYNC,
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )
        }
    }

    private val longPollingTimeout = 2.minutes
    private val nextLongPollingDelay = 1.minutes

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(-123456, createNotification())
    }

    override suspend fun doWork(): Result =
        withMatrixMessengerService(applicationContext) { matrixMultiMessenger ->
            val matrixMessenger = matrixMultiMessenger.activeMatrixMessenger.value
                ?: return@withMatrixMessengerService Result.failure()
            coroutineScope {
                log.debug { "start PushNotificationsForegroundWorker" }
                createReceivingNotificationChannel()

                // TODO what do we want: do the polling for clients, but restart polling if one/all (?) have problems
                val matrixClients = matrixMessenger.di.get<MatrixClients>()
                val settings = matrixMessenger.di.get<MatrixMessengerSettingsHolder>()
                matrixClients.collectLatest { currentMatrixClients ->
                    currentMatrixClients.forEach { (userId, matrixClient) ->
                        launch {
                            while (currentCoroutineContext().isActive &&
                                settings[userId].first()?.platformNotifications?.pushMode == PushMode.POLLING
                            ) {
                                val shouldBeRunning = applicationContext.backgroundSyncShouldBeRunning
                                log.debug { "initialSyncOnceFinished: ${matrixClient.syncState.value != SyncState.INITIAL_SYNC}, backgroundSyncShouldBeRunning: $shouldBeRunning" }
                                if (matrixClient.syncState.value != SyncState.INITIAL_SYNC &&
                                    shouldBeRunning // only sync when the activity is not in foreground
                                ) {
                                    log.debug { "polling: sync once" }

                                    matrixClient.syncOnce(timeout = longPollingTimeout)
                                        .onSuccess {
                                            log.debug { "polling: sync once was successful" }
                                        }
                                        .onFailure {
                                            log.error(it) { "cannot get sync response" }
                                        }
                                }

                                delay(nextLongPollingDelay)
                            }
                        }
                    }
                }
                Result.success()
            }
        }

    private fun createNotification(): Notification {
        val notification: Notification =
            NotificationCompat.Builder(applicationContext, CHANNEL_ID_RECEIVING).apply {
                setContentTitle("Empfange neue Nachrichten")
                priority = NotificationCompat.PRIORITY_LOW
                setSmallIcon(R.drawable.ic_logo)
                color = ContextCompat.getColor(applicationContext, R.color.logo)
                setSilent(true)
                setOngoing(true)
                setCategory(NotificationCompat.CATEGORY_SERVICE)
            }.build()
        return notification
    }

    private fun createReceivingNotificationChannel() {
        val name = applicationContext.getString(R.string.receiving_channel_name)
        val descriptionText = applicationContext.getString(R.string.receiving_channel_description)
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(CHANNEL_ID_RECEIVING, name, importance).apply {
            description = descriptionText
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            setSound(null, null)
            setShowBadge(false)
            setImportance(importance)
        }
        // Register the channel with the system
        val notificationManager: NotificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

}
