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
        data class Ready(val amplitudes: List<Float>) : State

        // TODO: Should we calculate the percentage of the audio played in the composable to reduce state flow emissions
        data class Playing(val progress: Float, val amplitudes: List<Float>) : State
        object Loading : State
    }
}
