package de.connect2x.trixnity.messenger.media

import de.connect2x.trixnity.messenger.util.toNSUrl
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.folivo.trixnity.client.media.okio.OkioPlatformMedia
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.AVURLAssetOverrideMIMETypeKey
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.seekToTime
import platform.CoreMedia.CMTimeMake
import platform.darwin.NSObject
import kotlin.time.Duration

private val log = KotlinLogging.logger {}

@OptIn(ExperimentalForeignApi::class)
internal class ApplePlayerItem(
    private val item: RoomMessageTimelineElementViewModel.FileBased<*>,
    playerState: StateFlow<MediaPlayer.State>,
    private val playerMutex: Mutex,
    private val coroutineScope: CoroutineScope,
    private val playingItem: StateFlow<ApplePlayerItem?>,
    private val withPlayer: (item: AVPlayerItem?, closure: (AVPlayer) -> Unit) -> Unit
) : NSObject(), MediaPlayer.Item {
    private val itemState: MutableStateFlow<MediaPlayer.State> = MutableStateFlow(MediaPlayer.State.Ready)
    private val file: MutableStateFlow<OkioPlatformMedia.TemporaryFile?> = MutableStateFlow(null)
    private val isAudio = item is RoomMessageTimelineElementViewModel.FileBased.Audio
    private val rawMimeType = if (isAudio) "audio/raw" else "video/raw"
    private var playerItem: AVPlayerItem? = null

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

    override suspend fun play(startPosition: Duration?) = withMediaFile { file ->
        playerMutex.withLock {
            if (isPlaying.value || state.value !is MediaPlayer.State.Ready) {
                log.error { "Unable to start playback: Media file is not in the state to be played" }
                return@withMediaFile
            }

            log.debug { "Stop previously played media file before starting to play new media file" }
            playingItem.value?.pause0() // Stop the currently playing element if one is currently played

            isPlaying.value = true
            log.debug { "Starting playback of media item '${item.name}'" }
            if (playerItem == null) {
                val options = mapOf<Any?, Any>(AVURLAssetOverrideMIMETypeKey to (item.mimeType ?: rawMimeType))
                val asset = AVURLAsset.URLAssetWithURL(file.path.toNSUrl(), options)
                playerItem = AVPlayerItem.playerItemWithAsset(asset)
            }

            withPlayer(requireNotNull(playerItem)) { player ->
                if (startPosition != null) {
                    player.seekToTime(CMTimeMake(startPosition.inWholeMilliseconds, 1000))
                }

                player.play()
            }
        }
    }

    override suspend fun pause() = playerMutex.withLock {
        if (!isPlaying.value || state.value !is MediaPlayer.State.Ready) {
            log.error { "Unable to stop playback: Media file is not in the state to be stopped" }
            return@withLock
        }

        pause0()
    }

    override suspend fun seekTo(position: Duration) {
        if (!isPlaying.value) {
            elapsedTime.value = position
            return
        }

        withPlayer(null) { player ->
            val time = CMTimeMake(position.inWholeMilliseconds, 1000)
            player.seekToTime(time)
        }
    }

    override fun close() {
        coroutineScope.launch {
            file.value?.delete()
        }
    }

    private fun pause0() = withPlayer(null) { player ->
        player.pause()
        isPlaying.value = false
    }

    private suspend fun withMediaFile(closure: suspend (OkioPlatformMedia.TemporaryFile) -> Unit) {
        val tempFile = file.value
        if (tempFile != null) {
            closure(tempFile)
            return
        }

        log.debug { "No media found, downloading media file...." }
        item.downloadMedia(
            onDownloadCancelled = {},
            processFile = { media ->
                check(media is OkioPlatformMedia) { "Media must be a OkioPlatformMedia" }
                media.getTemporaryFile().fold(
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
            },
        )
    }
}
