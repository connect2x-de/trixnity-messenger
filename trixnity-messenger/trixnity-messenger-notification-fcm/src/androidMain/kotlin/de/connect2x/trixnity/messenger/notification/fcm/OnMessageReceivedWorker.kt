package de.connect2x.trixnity.messenger.notification.fcm

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId

class OnMessageReceivedWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    companion object {

        fun enqueueWork(context: Context, profile: String?, account: String?, roomId: String, eventId: String?) {
            val workRequest = OneTimeWorkRequestBuilder<OnMessageReceivedWorker>()
                .setInputData(
                    workDataOf(
                        "profile" to profile,
                        "account" to account,
                        "roomId" to roomId,
                        "eventId" to eventId,
                    )
                )
                .build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }

    override suspend fun doWork(): Result {
        val profile = inputData.getString("profile")
        val account = inputData.getString("account")?.let(::UserId)
        val roomId = inputData.getString("roomId")?.let(::RoomId) ?: return Result.failure()
        val eventId = inputData.getString("eventId")?.let(::EventId)
        
        withFcmPushNotificationProvider(context) {
            val didAlreadyProcessOnPush = it.onPush(profile, account, roomId, eventId)
            if (!didAlreadyProcessOnPush) it.processPending(profile, account)
        }
        return Result.success()
    }
}
