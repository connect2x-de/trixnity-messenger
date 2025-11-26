package de.connect2x.trixnity.messenger.viewmodel.media

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration

interface AudioPlayerViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        audio: RoomMessageTimelineElementViewModel.FileBased.Audio
    ) : AudioPlayerViewModel
}

interface AudioPlayerViewModel {
    val elapsedTime: StateFlow<Duration>
    val state: StateFlow<State>

    fun start()
    fun stop()

    /**
     * Loading -> Failed / Ready -> Playing <-> Ready
     */
    sealed interface State {
        data class Failed(val cause: Throwable? = null) : State
        data class Ready(val amplitudes: Array<Float>) : State {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other == null || this::class != other::class) return false

                other as Ready

                if (!amplitudes.contentEquals(other.amplitudes)) return false

                return true
            }

            override fun hashCode(): Int {
                return amplitudes.contentHashCode()
            }
        }

        data class Playing(val progress: Float, val amplitudes: Array<Float>) : State {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other == null || this::class != other::class) return false

                other as Playing

                if (progress != other.progress) return false
                if (!amplitudes.contentEquals(other.amplitudes)) return false

                return true
            }

            override fun hashCode(): Int {
                var result = progress.hashCode()
                result = 31 * result + amplitudes.contentHashCode()
                return result
            }
        }

        object Loading : State
    }
}
