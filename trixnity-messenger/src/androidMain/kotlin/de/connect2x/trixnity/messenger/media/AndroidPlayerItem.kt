package de.connect2x.trixnity.messenger.media

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import de.connect2x.trixnity.client.media.okio.OkioPlatformMedia
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal class AndroidPlayerItem(
    override val id: String,
    override val duration: Duration,
    mimeType: String,
    private val tempFile: OkioPlatformMedia.TemporaryFile,
    private val coroutineScope: CoroutineScope,
    private val player: AndroidMediaPlayer
) : AbstractMediaItem(coroutineScope, player.playingItemMutex, player.currentItemPlaying) {
    private val item = MediaItem.Builder().setMediaId(tempFile.path.toString()).setMimeType(mimeType).build()
    private var updateJob: Job? = null

    override fun onPlay(duration: Duration): Result<Unit> {
        player.withMediaController { controller ->
            controller.setMediaItem(item, duration.inWholeMilliseconds)
            controller.prepare()
            controller.play()
            updateJob = coroutineScope.launch {
                while (isActive) {
                    delay(150)
                    player.withMediaController { controller ->
                        elapsedTime.value = controller.currentPosition.coerceAtLeast(0).milliseconds
                    }
                }
            }
        }

        return Result.success(Unit)
    }

    override fun onSeekTo(position: Duration) = player.withMediaController { controller ->
        if (!controller.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)) {
            log.error { "Unable to seek media playback because seek is not available" }
            return@withMediaController
        }

        log.debug { "Seeking media playback to position $position" }
        val index = controller.currentMediaItemIndex.takeIf { it != C.INDEX_UNSET } ?: 0
        controller.seekTo(index, position.inWholeMilliseconds)
    }

    override fun onPause() = player.withMediaController { controller ->
        controller.pause()
        controller.clearMediaItems()
        updateJob?.cancel()
        updateJob = null
    }

    override suspend fun onClose() {
        tempFile.delete()
        coroutineScope.cancel()
    }
}
