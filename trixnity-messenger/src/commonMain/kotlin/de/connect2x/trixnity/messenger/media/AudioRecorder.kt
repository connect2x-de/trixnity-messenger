package de.connect2x.trixnity.messenger.media

import de.connect2x.trixnity.client.media.PlatformMedia
import de.connect2x.trixnity.messenger.util.ExperimentalTrixnityMessengerApi
import io.ktor.http.ContentType
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration

@ExperimentalTrixnityMessengerApi
interface AudioRecorder : AutoCloseable {
    val state: StateFlow<State>
    
    suspend fun startSuspending()
    fun complete()

    sealed interface State {
        object Ready : State

        data class Recording(val duration: Duration, val loudness: Float) : State

        data class Completed(
            val data: PlatformMedia,
            val duration: Duration,
            val sizeBytes: Long?,
            val contentType: ContentType,
        ) : State
    }
}

@OptIn(ExperimentalTrixnityMessengerApi::class)
class AudioRecorderHolder(val getOrNull: AudioRecorder?)
