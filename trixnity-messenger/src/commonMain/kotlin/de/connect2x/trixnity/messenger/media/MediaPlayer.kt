package de.connect2x.trixnity.messenger.media

import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import net.folivo.trixnity.client.media.PlatformMedia
import kotlin.time.Duration

interface MediaPlayer : AutoCloseable {
    val playingItem: StateFlow<Item?>
    val state: StateFlow<State>

    /**
     * @param media          the platform media containing a file
     * @param mimeType       the mime type of the platform media
     * @param coroutineScope the coroutine scope of the media
     * @return               the created media item or an error
     */
    suspend fun open(media: PlatformMedia, mimeType: String, coroutineScope: CoroutineScope): Result<Item>

    interface Item : AutoCloseable {
        val isPlaying: StateFlow<Boolean>
        val duration: StateFlow<Duration?>
        val elapsedTime: StateFlow<Duration?>
        val state: StateFlow<State>

        /**
         * This function stops the currently played media (if there is one playing) and starts this media at the
         * position specified, alternatively the elapsed time position. When being played, this function changes the
         * `playingItem` state of the media player.
         *
         * @param startPosition the override for the starting position when playing
         */
        suspend fun play(startPosition: Duration? = null)

        /**
         * This function pauses this item when currently played. It also changes the `playingItem` state of the media
         * player.
         */
        suspend fun pause()
        suspend fun seekTo(position: Duration)
    }

    sealed interface State {
        object Ready : State
        class Failed(val message: String) : State
    }

}
