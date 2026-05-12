package de.connect2x.trixnity.messenger

import android.content.Context
import android.graphics.BitmapFactory
import androidx.lifecycle.asFlow
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import de.connect2x.sysnotify.Notification
import de.connect2x.sysnotify.NotificationHandle
import de.connect2x.sysnotify.NotificationIcon
import de.connect2x.sysnotify.create
import de.connect2x.sysnotify.fromBitmap
import de.connect2x.sysnotify.notification
import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.InitialSyncWorker.Companion.UNIQUE_WORK_NAME
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.notification.NotificationHandlers
import de.connect2x.trixnity.messenger.util.ContextGetter
import de.connect2x.trixnity.messenger.viewmodel.initialsync.RunInitialSync
import de.connect2x.trixnity.messenger.viewmodel.initialsync.RunInitialSyncImpl
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.job
import org.koin.dsl.module

class InitialSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    companion object {
        const val UNIQUE_WORK_NAME = "initialSync"

        fun enqueueUniqueWork(context: Context, account: String) {
            val workRequest = OneTimeWorkRequestBuilder<InitialSyncWorker>()
                .setInputData(Data.Builder().apply {
                    putString("account", account)
                }.build())
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setConstraints(
                    Constraints.Builder()
                        .build()
                )
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork("$UNIQUE_WORK_NAME-$account", ExistingWorkPolicy.KEEP, workRequest)
        }

        fun cancelUniqueWork(context: Context, account: String) {
            WorkManager.getInstance(context).cancelUniqueWork("$UNIQUE_WORK_NAME-$account")
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo =
        withMatrixMessengerFromService(applicationContext) { matrixMessenger ->
            val i18n = matrixMessenger.di.get<I18n>()
            val config = matrixMessenger.di.get<MatrixMessengerConfiguration>()
            // Load app icon for persistent notification if present
            val icon = config.appIcon?.let(applicationContext.assets::open)?.use { stream ->
                NotificationIcon.fromBitmap(BitmapFactory.decodeStream(stream))
            }
            matrixMessenger.di.get<NotificationHandlers>().global.create(
                Notification(
                    title = i18n.initialSyncNotificationTitle(),
                    description = i18n.initialSyncNotificationDescription(),
                    dismissible = false,
                    statusIcon = icon,
                    icon = icon
                )
            ) {}.toForegroundInfo()
        }

    override suspend fun doWork(): Result {
        val userId: UserId = inputData.getString("account")?.let(::UserId) ?: return Result.failure()
        return withMatrixMessengerFromService(applicationContext) { matrixMessenger ->

            val matrixClient = matrixMessenger.di.get<MatrixClients>().value[userId]
                ?: return@withMatrixMessengerFromService Result.failure()
            val success = RunInitialSyncImpl(matrixClient)

            if (success) Result.success() else Result.failure()
        }
    }
}

fun initialSyncModule() = module {
    single<RunInitialSync> {
        val contextGetter = get<ContextGetter>()
        object : RunInitialSync {
            override suspend fun invoke(matrixClient: MatrixClient): Boolean = coroutineScope {
                val context = contextGetter()
                val account = matrixClient.userId.full
                currentCoroutineContext().job.invokeOnCompletion {
                    InitialSyncWorker.cancelUniqueWork(context = context, account = account)
                }
                val result =
                    async {
                        WorkManager.getInstance(context).getWorkInfosForUniqueWorkLiveData("$UNIQUE_WORK_NAME-$account")
                            .asFlow()
                            .mapNotNull { it.firstOrNull() }
                            .filter { it.state.isFinished }
                            .first()
                    }

                InitialSyncWorker.enqueueUniqueWork(context = context, account = account)

                when (result.await().state) {
                    WorkInfo.State.ENQUEUED,
                    WorkInfo.State.RUNNING,
                    WorkInfo.State.SUCCEEDED -> true

                    WorkInfo.State.FAILED,
                    WorkInfo.State.BLOCKED,
                    WorkInfo.State.CANCELLED -> false
                }
            }
        }
    }
}

internal fun NotificationHandle.toForegroundInfo(): ForegroundInfo {
    return ForegroundInfo(hashCode(), notification)
}
