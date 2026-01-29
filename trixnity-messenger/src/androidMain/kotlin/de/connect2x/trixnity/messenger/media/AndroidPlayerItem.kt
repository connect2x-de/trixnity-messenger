package de.connect2x.trixnity.messenger.media

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.folivo.trixnity.client.media.okio.OkioPlatformMedia
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration

private val log = KotlinLogging.logger { }

@OptIn(UnstableApi::class)
internal class AndroidPlayerItem(
    private val item: RoomMessageTimelineElementViewModel.FileBased<*>,
    playerState: StateFlow<MediaPlayer.State>,
    private val coroutineScope: CoroutineScope,
    private val playingItemMutex: Mutex,
    private val playingItem: MutableStateFlow<AndroidPlayerItem?>,
    private val withMediaController: suspend (suspend (MediaController) -> Unit) -> Unit
) : MediaPlayer.Item {
    private val isAudio = item is RoomMessageTimelineElementViewModel.FileBased.Audio
    private val mimeType = item.mimeType ?: if (isAudio) MimeTypes.AUDIO_RAW else MimeTypes.VIDEO_RAW
    private val file: MutableStateFlow<OkioPlatformMedia.TemporaryFile?> = MutableStateFlow(null)
    private val itemState: MutableStateFlow<MediaPlayer.State> = MutableStateFlow(MediaPlayer.State.Ready)

    override val isPlaying: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val duration: MutableStateFlow<Duration?> = MutableStateFlow(null)
    override val elapsedTime: MutableStateFlow<Duration?> = MutableStateFlow(null)
    override val state: StateFlow<MediaPlayer.State> =
        combine(itemState, playerState) { itemState, playerState ->
            when (playerState) {
                is MediaPlayer.State.Failed -> playerState
                else -> itemState
            }
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), MediaPlayer.State.Ready)

    override suspend fun play(startPosition: Duration?): Unit = playingItemMutex.withLock {
        withMediaFile { tempFile ->
            if (isPlaying.value || state.value !is MediaPlayer.State.Ready) {
                log.error { "Unable to start playback: Media file is not in the state to be played" }
                return@withMediaFile
            }

            log.debug { "Stop previously played media file before starting to play new media file" }
            playingItem.value?.pause0() // Stop the currently playing element if one is currently played

            isPlaying.value = true
            log.debug { "Starting playback of media item '${item.name}'" }
            withMediaController { controller ->
                log.trace { "Successfully acquired media player, Starting media playback" }
                val item = MediaItem.Builder()
                    .setMediaId(tempFile.path.toString())
                    .setMimeType(mimeType)
                    .build()

                val startDuration = startPosition ?: elapsedTime.value ?: Duration.ZERO
                controller.setMediaItem(item, startDuration.inWholeMilliseconds)
                controller.prepare()
                controller.play()
                playingItem.value = this
            }
        }
    }

    override suspend fun pause(): Unit = playingItemMutex.withLock {
        if (!isPlaying.value || state.value !is MediaPlayer.State.Ready) {
            log.error { "Unable to stop playback: Media file is not in the state to be stopped" }
            return@withLock
        }

        pause0()
    }

    override suspend fun seekTo(position: Duration): Unit = playingItemMutex.withLock {
        seekTo0(position)
    }

    private suspend fun seekTo0(position: Duration) {
        if (!isPlaying.value) {
            elapsedTime.value = position
            return
        }

        withMediaController { controller ->
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
        withMediaController { controller ->
            log.trace { "Successfully acquired media player, stopping media playback" }
            playingItem.value = null
            isPlaying.value = false
            controller.pause()
            controller.clearMediaItems()
        }

        playingItem.value = null
    }

    override fun close() {
        coroutineScope.launch {
            file.value?.delete()
        }
    }

    private suspend fun withMediaFile(closure: suspend (OkioPlatformMedia.TemporaryFile) -> Unit) {
        suspendCancellableCoroutine { continuation ->
            item.downloadMedia(
                onDownloadCancelled = {
                    if (continuation.isActive) {
                        continuation.cancel(Exception("Download cancelled"))
                    }
                },
                processFile = { media ->
                    try {
                        check(media is OkioPlatformMedia) { "Media must be a OkioPlatformMedia" }
                        continuation.resume(media.getTemporaryFile())
                    } catch (e: Throwable) {
                        continuation.resumeWithException(e)
                    }
                }
            )
        }.fold(
            onFailure = {
                log.error(it) { "Unexpected error when acquiring file for playback" }
                itemState.value = MediaPlayer.State.Failed("Unable to download media: $it")
            },
            onSuccess = {
                log.debug { "Successfully downloaded media file" }
                file.value = it
                closure(it)
            }
        )
    }
}
