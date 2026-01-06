package de.connect2x.trixnity.messenger.notification.fcm

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
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

class SyncAndProcessPendingWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    companion object {
        const val UNIQUE_WORK_NAME = "syncAndProcessPending"
        const val INTERVAL_SETTINGS_KEY =
            "de.connect2x.trixnity.messenger.notification.fcm.SyncAndProcessPendingInterval"

        fun enqueueUniquePeriodicWork(context: Context) {
            val serviceEnabled =
                try {
                    context.packageManager.getServiceInfo(
                        ComponentName(
                            context,
                            TrixnityMessengerFirebaseMessagingService::class.java
                        ), 0
                    ).enabled
                } catch (_: PackageManager.NameNotFoundException) {
                    false
                }
            if (serviceEnabled.not()) return
            val interval = context.packageManager.getServiceInfo(
                ComponentName(context, TrixnityMessengerFirebaseMessagingService::class.java),
                PackageManager.GET_META_DATA
            ).metaData?.getInt(INTERVAL_SETTINGS_KEY)?.minutes ?: 15.minutes
            val workRequest = PeriodicWorkRequestBuilder<SyncAndProcessPendingWorker>(interval.toJavaDuration())
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(UNIQUE_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, workRequest)
        }

        fun stopUniquePeriodicWork(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
        }
    }

    override suspend fun doWork(): Result {
        withFcmPushNotificationProvider(context) {
            if (it.isEnabled.value) it.possiblySyncAndProcessPending()
            else stopUniquePeriodicWork(context) // BroadcastReceiver may not know that we are not active
        }

        return Result.success()
    }
}
