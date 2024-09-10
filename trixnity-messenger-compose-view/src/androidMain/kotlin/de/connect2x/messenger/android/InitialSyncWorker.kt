package de.connect2x.messenger.android

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import de.connect2x.messenger.compose.view.R
import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.viewmodel.initialsync.RunInitialSync
import io.github.oshai.kotlinlogging.KotlinLogging
import net.folivo.trixnity.core.model.UserId

private val log = KotlinLogging.logger { }

class InitialSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val userId = inputData.getString("userId")?.let(::UserId)
        ?: throw IllegalStateException("expected worker to have input data field 'accountName")

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(-1234567, createNotification())
    }

    override suspend fun doWork(): Result {
        withMatrixMessengerService(applicationContext) { matrixMultiMessenger ->
            val matrixMessenger = matrixMultiMessenger.activeMatrixMessenger.value
                ?:return Result.failure()
            log.debug { "start initial sync worker" }

            val matrixClient = matrixMessenger.di.get<MatrixClients>().value[userId] ?: return Result.failure()
            log.debug { "initial sync for $userId" }
            val success = RunInitialSync(matrixClient)
            log.debug { "initial sync done: $success" }
            return if (success) Result.success() else Result.failure()
        }
    }

    private fun createNotification(): Notification {
        val notification: Notification =
            NotificationCompat.Builder(applicationContext, CHANNEL_ID_INITIAL_SYNC)
                .apply {
                    setContentTitle("Laden des Kontos $userId")
                    priority = NotificationCompat.PRIORITY_LOW
                    setSmallIcon(R.drawable.ic_logo)
                    color = ContextCompat.getColor(applicationContext, R.color.logo)
                    setSilent(true)
                    setOngoing(true)
                    setCategory(NotificationCompat.CATEGORY_PROGRESS)
                    setProgress(100, 0, true)
                }.build()
        return notification
    }
}
