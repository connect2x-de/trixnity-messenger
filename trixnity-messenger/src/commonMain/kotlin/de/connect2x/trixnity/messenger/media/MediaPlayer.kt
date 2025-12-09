package de.connect2x.trixnity.messenger.media

import kotlinx.coroutines.flow.StateFlow
import net.folivo.trixnity.client.media.PlatformMedia
import kotlin.time.Duration

interface MediaPlayer : AutoCloseable {
    val elapsedTime: StateFlow<Duration>
    val duration: StateFlow<Duration>
    val isPlaying: StateFlow<Boolean>

    suspend fun start(
        media: PlatformMedia,
        mimeType: String? = null,
        position: Duration = Duration.ZERO,
        eventCallback: (Event) -> Unit = {}
    )

    suspend fun stop()
    suspend fun seekTo(position: Duration)

    sealed interface Event {
        data class Progress(val elapsedTime: Duration, val duration: Duration) : Event
        object Stopped : Event
    }
}
