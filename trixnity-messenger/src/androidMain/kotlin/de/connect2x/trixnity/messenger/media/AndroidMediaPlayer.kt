package de.connect2x.trixnity.messenger.media

import android.content.ComponentName
import android.media.MediaMetadataRetriever
import androidx.core.content.ContextCompat
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.lognity.api.logger.error
import de.connect2x.trixnity.client.media.PlatformMedia
import de.connect2x.trixnity.client.media.okio.OkioPlatformMedia
import de.connect2x.trixnity.messenger.util.ContextGetter
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.concurrent.atomics.AtomicReference
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal val Player.currentElapsedTime: Duration
    get() = currentPosition.coerceAtLeast(0).milliseconds

internal class AndroidMediaPlayer(getContext: ContextGetter, private val coroutineScope: CoroutineScope) : MediaPlayer {
    private val log: Logger = Logger("de.connect2x.trixnity.messenger.media.AndroidMediaPlayer")
    private val controller: ListenableFuture<MediaController>
    private val retainedItem: AtomicReference<AndroidPlayerItem?> = AtomicReference(null)

    internal val currentItemPlaying: MutableStateFlow<AndroidPlayerItem?> = MutableStateFlow(null)
    internal val playingItemMutex: Mutex = Mutex()

    override val playingItem: StateFlow<MediaPlayer.Item?> = currentItemPlaying.asStateFlow()

    init {
        val context = getContext()
        val sessionToken = SessionToken(context, ComponentName(context, MediaPlayerService::class.java))
        controller = MediaController.Builder(context, sessionToken).buildAsync()
        controller.addListener(
            { controller.get().let { it.addListener(ControllerEventListener(it)) } },
            ContextCompat.getMainExecutor(context),
        )
    }

    override suspend fun open(
        id: String,
        media: PlatformMedia,
        mimeType: String,
        lifecycleScope: CoroutineScope?,
    ): Result<MediaPlayer.Item> {
        check(media is OkioPlatformMedia) { "PlatformMedia is required to be a OkioPlatformMedia" }

        retainedItem
            .load()
            ?.takeIf { it.id == id }
            ?.let { item ->
                if (item.restore(lifecycleScope)) return Result.success(item)
                release(item)
            }

        currentItemPlaying.value
            ?.takeIf { it.id == id }
            ?.let { playingItem ->
                playingItem.updateLifecycle(lifecycleScope)
                return Result.success(playingItem)
            }

        val tempFile =
            media.getTemporaryFile().getOrElse {
                log.error(it) { "Unable to open media as temporary file" }
                return Result.failure(it)
            }
        log.debug { "Successfully opened media as temporary file" }
        return createPlayerItem(id, mimeType, tempFile, lifecycleScope)
    }

    override fun close() {
        currentItemPlaying.value = null
        coroutineScope.launch {
            try {
                withMediaController { controller -> controller.clearMediaItems() }
            } finally {
                try {
                    withContext(NonCancellable) { retainedItem.load()?.let { release(it, forceDelete = true) } }
                } finally {
                    coroutineScope.cancel()
                }
            }
        }
    }

    internal fun isRetained(item: AndroidPlayerItem): Boolean = retainedItem.load() === item

    internal suspend fun retain(item: AndroidPlayerItem) {
        val previousItem = retainedItem.exchange(item) ?: return
        if (isRetained(previousItem)) return
        previousItem.clearRetainedSeekPosition()
        previousItem.releaseFromController()
    }

    internal suspend fun withMediaController(closure: suspend (MediaController) -> Unit): Unit =
        try {
            val controller = withContext(Dispatchers.IO) { controller.get(10, TimeUnit.SECONDS) }
            withContext(Dispatchers.Main) { closure(controller) }
        } catch (ex: TimeoutException) {
            log.error(ex) { "Failed to acquire media controller: Unable to init player in 10 seconds" }
        }

    private fun createPlayerItem(
        id: String,
        mimeType: String,
        tempFile: OkioPlatformMedia.TemporaryFile,
        lifecycleScope: CoroutineScope?,
    ): Result<MediaPlayer.Item> {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(tempFile.path.toString())
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0
            if (duration <= 0) {
                return Result.failure(IllegalArgumentException("Media duration could not be extracted"))
            }

            val playerItem =
                AndroidPlayerItem(
                    id = id,
                    mimeType = mimeType,
                    tempFile = tempFile,
                    coroutineScope = coroutineScope,
                    player = this,
                    duration = duration.milliseconds,
                )

            playerItem.updateLifecycle(lifecycleScope)
            Result.success(playerItem)
        } catch (error: Exception) {
            Result.failure(IllegalArgumentException("Illegal media specified", error))
        } finally {
            retriever.release()
        }
    }

    private suspend fun release(item: AndroidPlayerItem, forceDelete: Boolean = false) = playingItemMutex.withLock {
        if (!retainedItem.compareAndSet(item, null)) return@withLock
        currentItemPlaying.compareAndSet(item, null)
        item.releaseFromController(forceDelete)
    }

    private inner class ControllerEventListener(private val mediaController: MediaController) : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            val item = retainedItem.load() ?: return
            if (isPlaying) {
                coroutineScope.launch {
                    if (!isRetained(item)) return@launch
                    item.synchronizePlayingFromController()
                }
                return
            }

            val playbackEnded = mediaController.playbackState == Player.STATE_ENDED
            val playbackStopped = mediaController.playbackState == Player.STATE_IDLE
            if (!playbackEnded && !playbackStopped && mediaController.playWhenReady) return

            coroutineScope.launch {
                if (!isRetained(item)) return@launch
                item.synchronizePausedFromController()
                if (playbackEnded) {
                    item.elapsedTime.value = Duration.ZERO
                    release(item)
                    withMediaController { it.clearMediaItems() }
                }
            }
        }

        override fun onEvents(player: Player, events: Player.Events) {
            if (events.contains(Player.EVENT_TIMELINE_CHANGED) && player.mediaItemCount == 0) {
                retainedItem.load()?.let { item ->
                    coroutineScope.launch {
                        item.synchronizePausedFromController()
                        release(item)
                    }
                }
            }

            if (!events.contains(Player.EVENT_POSITION_DISCONTINUITY)) return

            val item = retainedItem.load()?.takeIf { it.mediaId == player.currentMediaItem?.mediaId } ?: return
            val position = player.currentElapsedTime
            if (item.confirmControllerPosition(position)) item.elapsedTime.value = position
        }

        override fun onPlayerError(error: PlaybackException) {
            currentItemPlaying.value?.setError(error.message ?: "Unknown error while playing")
        }
    }
}
