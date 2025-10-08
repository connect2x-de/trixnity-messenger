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
import net.folivo.trixnity.core.model.UserId

class ProcessPendingWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    companion object {
        const val UNIQUE_WORK_NAME = "processPending"

        fun enqueueUniqueWork(context: Context, profile: String?, account: String?) {
            val workRequest = OneTimeWorkRequestBuilder<ProcessPendingWorker>()
                .setInputData(
                    workDataOf(
                        "profile" to profile,
                        "account" to account,
                    )
                )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork("$UNIQUE_WORK_NAME-$profile-$account", ExistingWorkPolicy.KEEP, workRequest)
        }
    }

    override suspend fun doWork(): Result {
        val profile = inputData.getString("profile")
        val account = inputData.getString("account")?.let(::UserId)
        withFirebasePushNotificationProvider(context) {
            it.processPending(profile, account)
        }
        return Result.success()
    }
}
