package de.connect2x.trixnity.messenger.media

import android.view.Window
import android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
import de.connect2x.trixnity.messenger.util.ActivityGetter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.withContext

private class WindowRequests(val wasKeepingScreenOn: Boolean, var count: Int = 0)

private val windowRequests = mutableMapOf<Window, WindowRequests>()

internal fun ScreenWakeLock(getActivity: ActivityGetter): ScreenWakeLock {
    return AndroidScreenWakeLock(getActivity, Dispatchers.Main.immediate)
}

private class AndroidScreenWakeLock(
    private val getActivity: ActivityGetter,
    private val mainDispatcher: CoroutineDispatcher,
) : ScreenWakeLock {
    override suspend fun hold() {
        withContext(mainDispatcher) {
            val window = runCatching { getActivity().window }.getOrNull() ?: return@withContext
            val requests =
                windowRequests.getOrPut(window) {
                    val wasKeepingScreenOn = window.attributes.flags and FLAG_KEEP_SCREEN_ON != 0
                    window.addFlags(FLAG_KEEP_SCREEN_ON)
                    WindowRequests(wasKeepingScreenOn)
                }
            requests.count++
            try {
                awaitCancellation()
            } finally {
                if (--requests.count == 0) {
                    windowRequests.remove(window)
                    if (!requests.wasKeepingScreenOn) window.clearFlags(FLAG_KEEP_SCREEN_ON)
                }
            }
        }
    }
}
