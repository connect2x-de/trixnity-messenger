package de.connect2x.trixnity.messenger.viewmodel.media

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import kotlinx.coroutines.flow.StateFlow

interface AudioPlayerViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        audio: RoomMessageTimelineElementViewModel.FileBased.Audio
    ) : AudioPlayerViewModel
}

interface AudioPlayerViewModel {
    val position: StateFlow<Long>
    val state: StateFlow<State>

    fun start()
    fun stop()

    /**
     * Loading -> Failed / Ready -> Playing <-> Ready
     */
    sealed interface State {
        data class Failed(val cause: Throwable? = null) : State
        object Ready : State
        object Playing : State
        object Loading : State
    }
}
