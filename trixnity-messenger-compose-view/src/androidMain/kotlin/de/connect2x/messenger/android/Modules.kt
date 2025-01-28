package de.connect2x.messenger.android

import androidx.lifecycle.asFlow
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import de.connect2x.sysnotify.NotificationHandler
import de.connect2x.sysnotify.SysNotifyIntent
import de.connect2x.sysnotify.create
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerConfiguration
import de.connect2x.trixnity.messenger.viewmodel.initialsync.RunInitialSync
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import net.folivo.trixnity.client.MatrixClient
import org.koin.dsl.module

private val log = KotlinLogging.logger { }

fun MatrixMultiMessengerConfiguration.notificationModule() = module {
    single<NotificationHandlerProvider> {
        log.debug { "Creating notification handler provider" }
        NotificationHandlerProviderImpl()
    }
    single<NotificationHandler> {
        log.debug { "Creating default notification handler" }
        NotificationHandler.create(
            name = appName,
            id = getDefaultChannelId(appName),
            contextGetter = { get() },
            activationIntent = { context, notification ->
                SysNotifyIntent(context, MessengerActivity::class.java, notification)
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
                    .onEach { log.debug { "initial sync worker state: ${it?.state?.name}" } }
                    .filter { it?.state?.isFinished == true }
                    .map {
                        if (it?.state == WorkInfo.State.SUCCEEDED) {
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
