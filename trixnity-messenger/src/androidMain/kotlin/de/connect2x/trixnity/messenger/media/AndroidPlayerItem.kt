package de.connect2x.trixnity.messenger.media

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import de.connect2x.trixnity.client.media.okio.OkioPlatformMedia
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicReference
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock

internal class AndroidPlayerItem(
    override val id: String,
    override val duration: Duration,
    mimeType: String,
    private val tempFile: OkioPlatformMedia.TemporaryFile,
    private val coroutineScope: CoroutineScope,
    private val player: AndroidMediaPlayer,
) : AbstractMediaItem<AndroidPlayerItem>(coroutineScope, player.playingItemMutex, player.currentItemPlaying) {
    private val item = MediaItem.Builder().setMediaId(tempFile.path.toString()).setMimeType(mimeType).build()

    internal val mediaId: String
        get() = item.mediaId

    private val closeRequested = AtomicBoolean(false)
    private val tempFileDeleted = AtomicBoolean(false)
    private val pendingSeek = AtomicReference<PendingSeek?>(null)
    private var updateJob: Job? = null

    override val clearCurrentItemOnClose: Boolean = false

    override suspend fun onPlay(duration: Duration): Result<Unit> {
        player.withMediaController { controller ->
            player.currentItemPlaying.value = this
            player.retain(this)
            if (controller.currentMediaItem?.mediaId != item.mediaId) {
                pendingSeek.store(PendingSeek(duration))
                controller.setMediaItem(item, duration.inWholeMilliseconds)
                controller.prepare()
            } else if (controller.playbackState == Player.STATE_IDLE) {
                controller.prepare()
            }
            controller.play()
            startUpdatingExternalControls()
        }

        return Result.success(Unit)
    }

    override suspend fun onSeekTo(position: Duration) {
        log.debug { "Seeking media playback to position $position" }

        player.currentItemPlaying.value?.takeIf { it !== this }?.pauseWithoutLock()

        player.withMediaController { controller ->
            player.currentItemPlaying.value = this
            elapsedTime.value = position
            pendingSeek.store(PendingSeek(position))
            if (controller.currentMediaItem?.mediaId == item.mediaId) {
                controller.seekTo(position.inWholeMilliseconds)
            } else {
                controller.setMediaItem(item, position.inWholeMilliseconds)
                controller.prepare()
            }
            player.retain(this)
        }
    }

    override suspend fun onPause() = player.withMediaController { controller ->
        controller.pause()
        updateJob?.cancel()
        updateJob = null
    }

    /**
     * This function determines whether a position reported by the media controller may be applied to this item's
     * elapsed time. While a seek is pending, all controller positions are rejected until the controller confirms the
     * pending seek position, which prevents stale positions of previously seeked items from flickering through.
     */
    internal fun confirmControllerPosition(position: Duration): Boolean {
        val pending = pendingSeek.load() ?: return true
        return pending.position == position && pendingSeek.compareAndSet(pending, null)
    }

    /** This function clears the seek position retained by this item when another item is retained instead. */
    internal fun clearRetainedSeekPosition() {
        pendingSeek.store(null)
        elapsedTime.value = Duration.ZERO
    }

    internal suspend fun synchronizePlayingFromController() =
        player.playingItemMutex.withLock {
            if (
                !player.isRetained(this) ||
                    player.currentItemPlaying.value !== this ||
                    closeRequested.load() ||
                    state.value !is MediaPlayer.Item.State.Ready
            ) {
                return@withLock
            }
            player.currentItemPlaying.value = this
            state.value = MediaPlayer.Item.State.Playing
            startUpdatingExternalControls()
        }

    internal suspend fun synchronizePausedFromController() =
        player.playingItemMutex.withLock {
            if (
                !player.isRetained(this) ||
                    player.currentItemPlaying.value !== this ||
                    state.value !is MediaPlayer.Item.State.Playing
            ) {
                return@withLock
            }
            pauseWithoutLock()
        }

    internal suspend fun restore(lifecycleScope: CoroutineScope?): Boolean {
        reopenAfterClose()

        return player.playingItemMutex.withLock {
            if (!player.isRetained(this)) return@withLock false

            var restored = false
            player.withMediaController { controller ->
                if (
                    !player.isRetained(this) ||
                        controller.currentMediaItem?.mediaId != item.mediaId ||
                        controller.playbackState == Player.STATE_ENDED
                ) {
                    return@withMediaController
                }

                closeRequested.store(false)
                pendingSeek.store(null)
                elapsedTime.value = controller.currentElapsedTime
                player.currentItemPlaying.value = this
                if (controller.playWhenReady && controller.playbackState != Player.STATE_IDLE) {
                    state.value = MediaPlayer.Item.State.Playing
                    startUpdatingExternalControls()
                } else {
                    state.value = MediaPlayer.Item.State.Ready
                }
                updateLifecycle(lifecycleScope)
                restored = true
            }
            restored
        }
    }

    override fun onClosing() {
        closeRequested.store(true)
    }

    override suspend fun onClose() {
        if (!player.isRetained(this)) deleteTempFile()
    }

    internal suspend fun releaseFromController(forceDelete: Boolean = false) {
        pendingSeek.store(null)
        if (forceDelete || closeRequested.load()) deleteTempFile()
    }

    private suspend fun deleteTempFile() {
        if (tempFileDeleted.compareAndSet(expectedValue = false, newValue = true)) tempFile.delete()
    }

    private fun startUpdatingExternalControls() {
        updateJob?.cancel()
        updateJob = coroutineScope.launch {
            while (isActive) {
                delay(150.milliseconds)
                player.withMediaController { controller ->
                    if (controller.currentMediaItem?.mediaId != item.mediaId) return@withMediaController
                    val position = controller.currentElapsedTime
                    if (confirmControllerPosition(position)) elapsedTime.value = position
                }
            }
        }
    }

    /**
     * A wrapper around a pending seek position. Because [Duration] is a value class, its boxed instances do not have a
     * stable identity, which makes them unsafe to use with [AtomicReference.compareAndSet]. This wrapper is a regular
     * class and therefore provides the stable identity required for compare-and-set operations.
     */
    private class PendingSeek(val position: Duration)
}
