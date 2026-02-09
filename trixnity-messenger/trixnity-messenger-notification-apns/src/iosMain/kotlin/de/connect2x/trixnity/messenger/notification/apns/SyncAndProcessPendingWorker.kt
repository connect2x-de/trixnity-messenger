package de.connect2x.trixnity.messenger.notification.apns

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.runBlocking
import platform.BackgroundTasks.BGProcessingTask
import platform.BackgroundTasks.BGProcessingTaskRequest
import platform.BackgroundTasks.BGTask
import platform.BackgroundTasks.BGTaskScheduler
import platform.Foundation.NSDate
import platform.Foundation.dateWithTimeIntervalSinceNow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

object SyncAndProcessPendingWorker {
    private const val UNIQUE_WORK_NAME = "de.connect2x.trixnity.messenger.notification.apns.SyncAndProcessPending"

    fun registerUniquePeriodicWork() {
        BGTaskScheduler.sharedScheduler()
            .registerForTaskWithIdentifier(identifier = UNIQUE_WORK_NAME, usingQueue = null) { task: BGTask? ->
                val processingTask = task as? BGProcessingTask
                if (processingTask != null) {
                    doWork(processingTask)
                } else {
                    task?.setTaskCompletedWithSuccess(true)
                }
            }
        enqueueUniquePeriodicWork(15.minutes)
    }

    fun enqueueUniquePeriodicWork(interval: Duration) {
        stopUniquePeriodicWork()
        scheduleUniquePeriodicWork(interval)
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun scheduleUniquePeriodicWork(interval: Duration) {
        val taskRequest = BGProcessingTaskRequest(UNIQUE_WORK_NAME)
        taskRequest.setRequiresNetworkConnectivity(true)
        taskRequest.setRequiresExternalPower(false)
        taskRequest.setEarliestBeginDate(NSDate.dateWithTimeIntervalSinceNow(interval.inWholeSeconds.toDouble()))
        BGTaskScheduler.sharedScheduler.submitTaskRequest(taskRequest, null)
    }

    fun stopUniquePeriodicWork() {
        BGTaskScheduler.sharedScheduler().cancelTaskRequestWithIdentifier(UNIQUE_WORK_NAME)
    }

    private fun doWork(task: BGProcessingTask) {
        try {
            runBlocking {
                withApnsPushNotificationProvider { provider ->
                    if (provider.isEnabled.value) {
                        scheduleUniquePeriodicWork(provider.config.periodicSyncInterval)
                        provider.possiblySyncAndProcessPending()
                    } else {
                        stopUniquePeriodicWork() // registerUniquePeriodicWork may not know that we are not active
                    }
                }
            }
            task.setTaskCompletedWithSuccess(true)
        } catch (_: Throwable) {
            task.setTaskCompletedWithSuccess(false)
        }
    }
}
