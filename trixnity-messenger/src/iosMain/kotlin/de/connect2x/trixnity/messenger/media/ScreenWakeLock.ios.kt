package de.connect2x.trixnity.messenger.media

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.withContext
import platform.UIKit.UIApplication

private var requestCount = 0
private var wasIdleTimerDisabled = false

internal fun ScreenWakeLock(): ScreenWakeLock {
    return AppleScreenWakeLock({ UIApplication.sharedApplication }, Dispatchers.Main.immediate)
}

private class AppleScreenWakeLock(
    private val getApplication: () -> UIApplication,
    private val mainDispatcher: CoroutineDispatcher,
) : ScreenWakeLock {
    override suspend fun hold() {
        withContext(mainDispatcher) {
            val application = getApplication()
            if (requestCount == 0) {
                wasIdleTimerDisabled = application.idleTimerDisabled
                application.idleTimerDisabled = true
            }
            requestCount++
            try {
                awaitCancellation()
            } finally {
                if (--requestCount == 0) {
                    application.idleTimerDisabled = wasIdleTimerDisabled
                }
            }
        }
    }
}
