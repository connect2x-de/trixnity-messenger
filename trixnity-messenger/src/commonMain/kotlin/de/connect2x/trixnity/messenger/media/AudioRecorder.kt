package de.connect2x.trixnity.messenger.media

import de.connect2x.trixnity.client.media.PlatformMedia
import de.connect2x.trixnity.messenger.util.ExposedImplementationDetailTrixnityMessenger
import io.ktor.http.ContentType
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration

interface AudioRecorder : AutoCloseable {
    val state: StateFlow<State>
    
    suspend fun startSuspending()
    fun complete()
    override fun close()
    
    sealed interface State {
        object Ready : State
        data class Recording(val duration: Duration, val loudness: Float) : State

        data class Completed(
            @property:ExposedImplementationDetailTrixnityMessenger val data: PlatformMedia,
            val duration: Duration,
            val sizeBytes: Long?,
            val contentType: ContentType,
        ) : State// TODO: Doc: Could be used to display preview
    }
}
