package de.connect2x.messenger.android

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.lifecycle.asFlow
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import de.connect2x.trixnity.messenger.viewmodel.initialsync.RunInitialSync
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import net.folivo.trixnity.client.MatrixClient
import org.koin.dsl.module

private val log = KotlinLogging.logger { }

fun initialSyncModule() = module {
    single<RunInitialSync> {
        object : RunInitialSync {
            override suspend fun invoke(matrixClient: MatrixClient): Boolean {
                createInitialSyncChannel(get())
                val workRequest = OneTimeWorkRequestBuilder<InitialSyncWorker>()
                    .setInputData(Data.Builder().apply {
                        putString("userId", matrixClient.userId.full)
                    }.build())
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build()
                    )
                    .build()

                val success = WorkManager.getInstance(get()).getWorkInfoByIdLiveData(workRequest.id).asFlow()
                    .onEach { log.debug { "initial sync worker state: ${it.state.name}" } }
                    .filter { it.state.isFinished }
                    .map {
                        if (it.state == WorkInfo.State.SUCCEEDED) {
                            log.debug { "operation success" }
                            true
                        } else {
                            log.error { "operation failure" }
                            false
                        }
                    }
                    .first()
                log.debug { "start initial sync" }
                // do NOT use unique work as multiple accounts can run the sync in parallel
                WorkManager.getInstance(get()).enqueue(workRequest)
                return success
            }

            fun createInitialSyncChannel(context: Context) {
                val name = "Initialer Sync"
                val descriptionText = "Einrichten von Timmy: Lade Kontodaten vom Server" // TODO i18n
                val importance = NotificationManager.IMPORTANCE_LOW
                val channel =
                    NotificationChannel(CHANNEL_ID_INITIAL_SYNC, name, importance).apply {
                        description = descriptionText
                        lockscreenVisibility = Notification.VISIBILITY_PRIVATE
                        setSound(null, null)
                        setShowBadge(false)
                    }
                // Register the channel with the system
                val notificationManager: NotificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }
    }
}
