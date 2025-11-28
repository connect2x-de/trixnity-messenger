package de.connect2x.trixnity.messenger.viewmodel.media

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration

interface AudioPlayerViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        audio: RoomMessageTimelineElementViewModel.FileBased.Audio,
        initialDuration: Duration?
    ) : AudioPlayerViewModel
}

interface AudioPlayerViewModel {
    val elapsedTime: StateFlow<Duration>
    val duration: StateFlow<Duration>
    val state: StateFlow<State>

    fun start()
    fun stop()
    fun seekTo(duration: Duration)

    /**
     * Loading -> Failed / Ready -> Playing <-> Ready
     */
    sealed interface State {
        data class Failed(val cause: Throwable? = null) : State
        object Loading : State

        /**
         * @param amplitudes the normalized amplitudes (0.0 to 1.0) of the audio file
         */
        data class Ready(val amplitudes: List<Float>) : State

        /**
         * @param amplitudes the normalized amplitudes (0.0 to 1.0) of the audio file
         */
        data class Playing(val amplitudes: List<Float>) : State
    }
}
