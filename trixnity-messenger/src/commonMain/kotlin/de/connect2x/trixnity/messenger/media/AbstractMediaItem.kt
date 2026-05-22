package de.connect2x.trixnity.messenger.media

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.lognity.api.logger.error
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * This class is the implementation of the part of the media item which handles and helps to commonise the state
 * transitioning and reduces code duplication amongst multiple item implementations.
 */
abstract class AbstractMediaItem(
    private val coroutineScope: CoroutineScope,
    private val operationMutex: Mutex,
    private val currentItemPlaying: MutableStateFlow<AbstractMediaItem?>,
) : MediaItemLifecycleImpl(coroutineScope), MediaPlayer.Item {
    private val isClosed: AtomicBoolean = AtomicBoolean(false)

    override val log: Logger = Logger("de.connect2x.trixnity.messenger.media.MediaPlayer.Item")
    override val state: MutableStateFlow<MediaPlayer.Item.State> = MutableStateFlow(MediaPlayer.Item.State.Ready)
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
        if (state.value !is MediaPlayer.Item.State.Ready) {
            log.error { "Unable to start playback: Media file is not in the state to be played" }
            return@withLock
        }

        currentItemPlaying.value?.let { prevItem ->
            log.debug { "Stop previously played media file before starting to play new media file" }
            prevItem.pauseWithoutLock()
        }

        log.debug { "Starting playback of media item" }
        onPlay(elapsedTime.value ?: Duration.ZERO)
            .fold(
                onFailure = {
                    log.error(it) { "Unable to start playback of media item" }
                    state.value =
                        MediaPlayer.Item.State.Failed(it.message ?: "Unexpected error while starting playback")
                },
                onSuccess = {
                    log.debug { "Successfully started playback of media item" }
                    currentItemPlaying.value = this
                    state.value = MediaPlayer.Item.State.Playing
                },
            )
    }

    override suspend fun pause() = operationMutex.withLock { pauseWithoutLock() }

    override suspend fun seekTo(position: Duration): Unit = operationMutex.withLock {
        if (state.value !is MediaPlayer.Item.State.Playing) {
            elapsedTime.value = position
            return
        }

        onSeekTo(position)
    }

    override fun close() {
        if (!isClosed.compareAndSet(expectedValue = false, newValue = true)) {
            return
        }

        log.debug { "Closing media item" }
        coroutineScope.launch {
            pause()
            onClose()
        }
    }

    protected suspend fun pauseWithoutLock() {
        if (state.value !is MediaPlayer.Item.State.Playing) {
            log.error { "Unable to stop playback: Media file is not in the state to be stopped" }
            return
        }

        log.debug { "Pausing playback of media file" }
        onPause()
        currentItemPlaying.value = null
        state.value = MediaPlayer.Item.State.Ready
    }
}
