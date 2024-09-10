package de.connect2x.messenger.android.push

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger { }

/**
 * Will start polling if activated for an account. We do not need to consider the push mode, since it is handled by
 * the `FcmPushService` (Firebase).
 */
class WakeUpBroadcastReceiver : BroadcastReceiver() {

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(contextOrNull: Context?, intentOrNull: Intent?) {
        contextOrNull?.let { context ->
            PollingNotificationsWorker.requestStart(context)
        }
    }
}