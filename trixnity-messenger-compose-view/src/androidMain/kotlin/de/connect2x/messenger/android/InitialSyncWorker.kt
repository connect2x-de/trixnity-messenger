package de.connect2x.messenger.android

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import de.connect2x.messenger.compose.view.R
import de.connect2x.sysnotify.Notification
import de.connect2x.sysnotify.NotificationHandler
import de.connect2x.sysnotify.SysNotifyIntent
import de.connect2x.sysnotify.create
import de.connect2x.sysnotify.getNotificationIcon
import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.viewmodel.initialsync.RunInitialSync
import io.github.oshai.kotlinlogging.KotlinLogging
import net.folivo.trixnity.core.model.UserId

class InitialSyncWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    private val log = KotlinLogging.logger { }
    private val userId: UserId = requireNotNull(
        inputData.getString("userId")?.let(::UserId)
    ) { "Expected worker to have input data field 'userId" }

    private val notificationHandler: NotificationHandler = NotificationHandler.create(
        name = AndroidI18n.notificationInitialSyncTitle(),
        id = INITIAL_SYNC_CHANNEL_ID,
        contextGetter = { applicationContext },
        activationIntent = { context, notification ->
            SysNotifyIntent(
                context,
                MessengerActivity::class.java,
                notification
            )
        }
    )

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return notificationHandler.create(
            Notification(
                title = AndroidI18n.notificationInitialSyncContentTitle(userId),
                description = AndroidI18n.notificationInitialSyncDescription(context.getString(R.string.app_name)),
                dismissible = false,
                icon = applicationContext.resources.getNotificationIcon(R.drawable.ic_logo)
            )
        ).toForegroundInfo()
    }

    override suspend fun doWork(): Result {
        withMatrixMessengerService(applicationContext) { matrixMultiMessenger ->
            val matrixMessenger = matrixMultiMessenger.activeMatrixMessenger.value
                ?: return Result.failure()
            log.debug { "Starting initial sync worker" }

            val matrixClient = matrixMessenger.di.get<MatrixClients>().value[userId] ?: return Result.failure()
            log.debug { "Initial sync for $userId" }
            val success = RunInitialSync(matrixClient)
            log.debug { "Initial sync done: $success" }

            notificationHandler.close() // Close notification handler when work is done
            return if (success) Result.success() else Result.failure()
        }
    }
}
