package de.connect2x.trixnity.messenger.notification.unifiedpush

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlin.time.Duration
import kotlin.time.toJavaDuration

class SyncAndProcessPendingWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    companion object {
        const val UNIQUE_WORK_NAME =
            "de.connect2x.trixnity.messenger.notification.unifiedpush.SyncAndProcessPendingWorker"

        fun enqueueUniquePeriodicWork(context: Context, interval: Duration) {
            val serviceEnabled =
                try {
                    context.packageManager.getServiceInfo(
                        ComponentName(
                            context,
                            TrixnityMessengerUnifiedPushService::class.java
                        ), 0
                    ).enabled
                } catch (_: PackageManager.NameNotFoundException) {
                    false
                }
            if (serviceEnabled.not()) return
            val workRequest = PeriodicWorkRequestBuilder<SyncAndProcessPendingWorker>(interval.toJavaDuration())
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .setInputData(workDataOf("interval" to interval.inWholeSeconds))
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(UNIQUE_WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, workRequest)
        }

        fun stopUniquePeriodicWork(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
        }
    }

    override suspend fun doWork(): Result {
        val currentInterval = inputData.getLong("interval", -1)
        withUnifiedPushNotificationProvider(context) {
            if (it.isEnabled.value) {
                if (currentInterval != it.config.periodicSyncInterval.inWholeSeconds) {
                    enqueueUniquePeriodicWork(context, interval = it.config.periodicSyncInterval)
                }
                it.possiblySyncAndProcessPending()
            } else stopUniquePeriodicWork(context) // BroadcastReceiver may not know that we are not active
        }

        return Result.success()
    }
}
