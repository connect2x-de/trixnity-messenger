package de.connect2x.trixnity.messenger.media

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import net.folivo.trixnity.client.media.okio.OkioPlatformMedia
import kotlin.time.Duration

private val log = KotlinLogging.logger { }

@OptIn(UnstableApi::class)
internal class AndroidPlayerItem(
    private val mimeType: String,
    private val tempFile: OkioPlatformMedia.TemporaryFile,
    private val coroutineScope: CoroutineScope,
    private val player: AndroidMediaPlayer,
) : MediaPlayer.Item {
    private val itemState: MutableStateFlow<MediaPlayer.State> = MutableStateFlow(MediaPlayer.State.Ready)

    override val isPlaying: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val duration: MutableStateFlow<Duration?> = MutableStateFlow(null)
    override val elapsedTime: MutableStateFlow<Duration?> = MutableStateFlow(null)
    override val state: StateFlow<MediaPlayer.State> =
        combine(itemState, player.state) { itemState, playerState ->
            when (playerState) {
                is MediaPlayer.State.Failed -> playerState
                else -> itemState
            }
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), MediaPlayer.State.Ready)

    override suspend fun play(startPosition: Duration?): Unit = player.playingItemMutex.withLock {
        if (isPlaying.value || state.value !is MediaPlayer.State.Ready) {
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

            val startDuration = startPosition ?: elapsedTime.value ?: Duration.ZERO
            controller.setMediaItem(item, startDuration.inWholeMilliseconds)
            controller.prepare()
            controller.play()
            player.currentItemPlaying.value = this
            isPlaying.value = true
        }
    }

    override suspend fun pause(): Unit = player.playingItemMutex.withLock {
        if (!isPlaying.value || state.value !is MediaPlayer.State.Ready) {
            log.error { "Unable to stop playback: Media file is not in the state to be stopped" }
            return@withLock
        }

        pause0()
    }

    override suspend fun seekTo(position: Duration): Unit = player.playingItemMutex.withLock {
        seekTo0(position)
    }

    private suspend fun seekTo0(position: Duration) {
        if (!isPlaying.value) {
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
            isPlaying.value = false
            controller.pause()
            controller.clearMediaItems()
        }
    }

    override fun close() {
        coroutineScope.launch {
            pause0()
            tempFile.delete()
        }
    }
}
