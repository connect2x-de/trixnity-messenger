package de.connect2x.trixnity.messenger.media

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.lognity.api.logger.error
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration

/**
 * This class is the implementation of the part of the media item which handles and helps to commonise the state
 * transitioning and reduces code duplication amongst multiple item implementations.
 */
abstract class AbstractMediaItemLifecycle(
    private val coroutineScope: CoroutineScope,
    private val operationMutex: Mutex,
    private val currentItemPlaying: MutableStateFlow<AbstractMediaItemLifecycle?>
) : MediaLifecycleItemImpl(coroutineScope), MediaPlayer.Item {
    override val log: Logger = Logger("de.connect2x.trixnity.messenger.media.MediaPlayer.Item")

    override val state: MutableStateFlow<MediaPlayer.State> = MutableStateFlow(MediaPlayer.State.Ready)
    override val elapsedTime: MutableStateFlow<Duration?> = MutableStateFlow(null)

    /**
     * This function starts the playback of this media without locking a mutex. In order to get a full-featured media
     * player, you MUST implement this.
     *
     * @param duration the start duration determined by the abstract item based on the elapsed time value
     */
    protected abstract suspend fun onPlay(duration: Duration): Result<Unit>

    /**
     * This function seeks the currently-running playback of the media item and is only called when the item is in the
     * playing state. In order to get a full-featured media player, you MUST implement this.
     */
    protected abstract suspend fun onSeekTo(position: Duration)

    /**
     * This function pauses the media player without locking a mutex. In order to get a full-featured media player, you
     * MUST implement this.
     */
    protected abstract suspend fun onPause()

    /**
     * This function allows the implementation to close resources specific to the platform on which the player is
     * implemented.
     */
    protected open suspend fun onClose() {}

    override suspend fun play(): Unit = operationMutex.withLock {
        if (state.value !is MediaPlayer.State.Ready) {
            log.error { "Unable to start playback: Media file is not in the state to be played" }
            return@withLock
        }

        currentItemPlaying.value?.let { prevItem ->
            log.debug { "Stop previously played media file before starting to play new media file" }
            prevItem.pauseWithoutLock()
        }

        log.debug { "Starting playback of media item" }
        onPlay(elapsedTime.value ?: Duration.ZERO).fold(
            onFailure = {
                log.error(it) { "Unable to start playback of media item" }
            },
            onSuccess = {
                log.debug { "Successfully started playback of media item" }
                currentItemPlaying.value = this
                state.value = MediaPlayer.State.Playing
            }
        )
    }

    override suspend fun pause() = operationMutex.withLock {
        if (state.value !is MediaPlayer.State.Playing) {
            log.error { "Unable to stop playback: Media file is not in the state to be stopped" }
            return@withLock
        }

        log.debug { "Pausing playback of media file" }
        onPause()
        currentItemPlaying.value = null
        state.value = MediaPlayer.State.Ready
    }

    override suspend fun seekTo(position: Duration) = operationMutex.withLock {
        if (state.value !is MediaPlayer.State.Playing) {
            elapsedTime.value = position
            return
        }

        onSeekTo(position)
    }

    override fun close() {
        log.debug { "Closing media item" }
        coroutineScope.launch {
            pauseWithoutLock()
            onClose()
        }
    }

    protected suspend fun pauseWithoutLock() {
        onPause()
        currentItemPlaying.value = null
        state.value = MediaPlayer.State.Ready
    }
}
