package de.connect2x.messenger.android

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.asFlow
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import de.connect2x.sysnotify.NotificationHandler
import de.connect2x.sysnotify.create
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerConfiguration
import de.connect2x.trixnity.messenger.viewmodel.initialsync.RunInitialSync
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.core.model.UserId
import org.koin.dsl.module

private val log = KotlinLogging.logger { }

fun notificationModule(
    userId: UserId? = null,
    messengerConfig: MatrixMultiMessengerConfiguration,
) = module {
    // The default notification handler of the app should be available globally in the DI
    val channelId = if (userId == null) CHANNEL_ID_DEFAULT else pushChannelId(
        userId,
        messengerConfig
    )
    log.debug { "Creating notification handler with channel ID $channelId" }
    single<NotificationHandler> {
        NotificationHandler.create(
            name = messengerConfig.appName,
            id = channelId,
            contextGetter = { get() }, // This grabs the application context from the DI
            activationIntent = { _, notification ->
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("${messengerConfig.urlProtocol}://localhost/room/${notification.userData}")
                )
            }
        )
    }
}

fun initialSyncModule() = module {
    single<RunInitialSync> {
        object : RunInitialSync {
            override suspend fun invoke(matrixClient: MatrixClient): Boolean {
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
        }
    }
}
