package de.connect2x.trixnity.messenger.notification.fcm

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerSettingsHolder
import de.connect2x.trixnity.messenger.multi.update
import de.connect2x.trixnity.messenger.notification.PushNotificationProvider
import de.connect2x.trixnity.messenger.update
import de.connect2x.trixnity.messenger.withDiFromService

class OnNewTokenWorker(private val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    companion object {
        const val UNIQUE_WORK_NAME = "de.connect2x.trixnity.messenger.notification.fcm.OnNewTokenWorker"

        fun enqueueUniqueWork(context: Context, pushKey: String) {
            val workRequest =
                OneTimeWorkRequestBuilder<OnNewTokenWorker>()
                    .setInputData(workDataOf("pushKey" to pushKey))
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.REPLACE, workRequest)
        }
    }

    override suspend fun doWork(): Result {
        val pushKey = inputData.getString("pushKey") ?: return Result.failure()
        withDiFromService(context) { di ->
            val pusher =
                PushNotificationProvider.PusherSettings(
                    pushKey = pushKey,
                    url = di.get<FcmPushNotificationProviderConfig>().pushUrl,
                )
            val multiSettings = di.getOrNull<MatrixMultiMessengerSettingsHolder>()
            val settings = di.getOrNull<MatrixMessengerSettingsHolder>()
            if (multiSettings != null) {
                multiSettings.update<MatrixMultiMessengerNotificationProviderFcmSettings> { it.copy(pusher = pusher) }
            } else if (settings != null) {
                settings.update<MatrixMessengerNotificationProviderFcmSettings> { it.copy(pusher = pusher) }
            }
        }
        return Result.success()
    }
}
