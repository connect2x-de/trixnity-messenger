package de.connect2x.trixnity.messenger.media

internal fun interface ScreenWakeLock {
    suspend fun hold()
}
