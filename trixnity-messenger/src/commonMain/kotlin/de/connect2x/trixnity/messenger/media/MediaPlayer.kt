package de.connect2x.trixnity.messenger.media

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import net.folivo.trixnity.client.media.PlatformMedia
import kotlin.time.Duration

interface MediaPlayer : AutoCloseable {
    val playingItem: StateFlow<Item?>

    /**
     * @param media          the platform media containing a file
     * @param mimeType       the mime type of the platform media
     * @return               the created media item or an error
     */
    suspend fun open(id: String, media: PlatformMedia, mimeType: String, lifecycleScope: CoroutineScope): Result<Item>

    interface Item : AutoCloseable {
        val id: String
        val duration: Duration
        val elapsedTime: StateFlow<Duration?>
        val state: StateFlow<State>

        /**
         * This function stops the currently played media (if there is one playing) and starts this media at the
         * position specified, alternatively the elapsed time position. When being played, this function changes the
         * `playingItem` state of the media player.
         */
        suspend fun play()

        /**
         * This function pauses this item when currently played. It also changes the `playingItem` state of the media
         * player.
         */
        suspend fun pause()
        suspend fun seekTo(position: Duration)
    }

    sealed interface State {
        /**
         * Ready is the state of the media player item when the media is currently not being played and the item has no
         * failure.
         */
        object Ready : State
        object Playing : State
        class Failed(val message: String) : State
    }

}
