package de.connect2x.trixnity.messenger.media

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import net.folivo.trixnity.client.media.okio.OkioPlatformMedia
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private val log = KotlinLogging.logger { }

@OptIn(UnstableApi::class)
internal class AndroidPlayerItem(
    override val id: String,
    override val duration: Duration,
    private val mimeType: String,
    private val tempFile: OkioPlatformMedia.TemporaryFile,
    private val coroutineScope: CoroutineScope,
    private val player: AndroidMediaPlayer
) : MediaPlayer.Item {
    override val elapsedTime: MutableStateFlow<Duration?> = MutableStateFlow(null)
    override val state: MutableStateFlow<MediaPlayer.State> = MutableStateFlow(MediaPlayer.State.Ready)

    override suspend fun play(): Unit = player.playingItemMutex.withLock {
        if (state.value !is MediaPlayer.State.Ready) {
            log.error { "Unable to start playback: Media file is not in the state to be played" }
            return@withLock
        }

        // Stop the currently playing element if one is currently played
        player.currentItemPlaying.value?.let { previousItem ->
            log.debug { "Stop previously played media file before starting to play new media file" }
            previousItem.pause0()
        }

        log.debug { "Starting playback of media item" }
        player.withMediaController { controller ->
            log.trace { "Successfully acquired media player, Starting media playback" }
            val item = MediaItem.Builder()
                .setMediaId(tempFile.path.toString())
                .setMimeType(mimeType)
                .build()

            val startDuration = elapsedTime.value ?: Duration.ZERO
            controller.setMediaItem(item, startDuration.inWholeMilliseconds)
            controller.prepare()
            controller.play()
            player.currentItemPlaying.value = this
            state.value = MediaPlayer.State.Playing(coroutineScope.launch {
                while (isActive) {
                    delay(150)
                    player.withMediaController { controller ->
                        elapsedTime.value = controller.currentPosition.coerceAtLeast(0).milliseconds
                    }
                }
            })
        }
    }

    override suspend fun pause(): Unit = player.playingItemMutex.withLock {
        if (state.value !is MediaPlayer.State.Playing) {
            log.error { "Unable to stop playback: Media file is not in the state to be stopped" }
            return@withLock
        }

        pause0()
    }

    override suspend fun seekTo(position: Duration): Unit = player.playingItemMutex.withLock {
        if (state.value !is MediaPlayer.State.Playing) {
            elapsedTime.value = position
            return
        }

        player.withMediaController { controller ->
            if (!controller.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)) {
                log.error { "Unable to seek media playback because seek is not available" }
                return@withMediaController
            }

            log.debug { "Seeking media playback to position $position" }
            val index = controller.currentMediaItemIndex.takeIf { it != C.INDEX_UNSET } ?: 0
            controller.seekTo(index, position.inWholeMilliseconds)
        }
    }

    private suspend fun pause0() {
        player.withMediaController { controller ->
            log.trace { "Successfully acquired media player, stopping media playback" }
            player.currentItemPlaying.value = null
            controller.pause()
            controller.clearMediaItems()
            (state.value as MediaPlayer.State.Playing).updateJob?.cancel()
            state.value = MediaPlayer.State.Ready
        }
    }

    override fun close() {
        coroutineScope.launch {
            pause0()
            tempFile.delete()
            coroutineScope.cancel()
        }
    }
}
