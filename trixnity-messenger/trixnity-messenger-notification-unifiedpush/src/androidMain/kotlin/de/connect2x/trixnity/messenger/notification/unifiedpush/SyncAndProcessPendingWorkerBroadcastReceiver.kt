package de.connect2x.trixnity.messenger.notification.unifiedpush

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlin.time.Duration.Companion.minutes

class SyncAndProcessPendingWorkerBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED ||
            intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            SyncAndProcessPendingWorker.enqueueUniquePeriodicWork(context, 15.minutes)
        }
    }
}
