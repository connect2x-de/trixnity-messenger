package de.connect2x.trixnity.messenger.media

import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration

interface MediaPlayer : AutoCloseable {
    val playingItem: StateFlow<Item?>
    val state: StateFlow<State>

    fun open(media: RoomMessageTimelineElementViewModel.FileBased<*>): Item

    interface Item : AutoCloseable {
        val isPlaying: StateFlow<Boolean>
        val duration: StateFlow<Duration?>
        val elapsedTime: StateFlow<Duration?>
        val state: StateFlow<State>

        suspend fun play(startPosition: Duration? = null)
        suspend fun pause()
        suspend fun seekTo(position: Duration)
    }

    sealed interface State {
        object Ready : State
        class Failed(val message: String) : State
    }

}
