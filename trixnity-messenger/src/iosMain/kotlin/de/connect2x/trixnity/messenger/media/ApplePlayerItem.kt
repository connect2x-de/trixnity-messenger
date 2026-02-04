package de.connect2x.trixnity.messenger.media

import de.connect2x.trixnity.messenger.interop.observer.NSObjectObserverProtocol
import de.connect2x.trixnity.messenger.util.toNSUrl
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import net.folivo.trixnity.client.media.okio.OkioPlatformMedia
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.AVPlayerItemStatusReadyToPlay
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.AVURLAssetOverrideMIMETypeKey
import platform.AVFoundation.duration
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.seekToTime
import platform.CoreMedia.CMTimeMake
import platform.Foundation.NSKeyValueObservingOptionInitial
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.addObserver
import platform.Foundation.removeObserver
import platform.darwin.NSObject
import platform.darwin.NSObjectProtocol
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private val log = KotlinLogging.logger {}

@OptIn(ExperimentalForeignApi::class)
internal class ApplePlayerItem(
    private val tempFile: OkioPlatformMedia.TemporaryFile,
    private val mimeType: String,
    private val coroutineScope: CoroutineScope,
    private val player: AppleMediaPlayer
) : MediaPlayer.Item {
    private val itemState: MutableStateFlow<MediaPlayer.State> = MutableStateFlow(MediaPlayer.State.Ready)
    private val file: MutableStateFlow<OkioPlatformMedia.TemporaryFile?> = MutableStateFlow(null)

    private val statusObserver: ItemStatusObserver = ItemStatusObserver()
    private var playEndObserver: NSObjectProtocol? = null
    private var playerItem: AVPlayerItem? = null

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

    override suspend fun play(startPosition: Duration?) {
        player.playerMutex.withLock {
            if (isPlaying.value || state.value !is MediaPlayer.State.Ready) {
                log.error { "Unable to start playback: Media file is not in the state to be played" }
                return
            }

            log.debug { "Stop previously played media file before starting to play new media file" }
            player.currentItemPlaying.value?.pause0() // Stop the currently playing element if one is currently played

            isPlaying.value = true
            log.debug { "Starting playback of media item" }
            if (playerItem == null) {
                val options = mapOf<Any?, Any>(AVURLAssetOverrideMIMETypeKey to mimeType)
                val asset = AVURLAsset.URLAssetWithURL(tempFile.path.toNSUrl(), options)
                playerItem = AVPlayerItem.playerItemWithAsset(asset)
                playerItem?.addObserver(statusObserver, "status", NSKeyValueObservingOptionInitial, null)
                playEndObserver = NSNotificationCenter.defaultCenter.addObserverForName(
                    name = AVPlayerItemDidPlayToEndTimeNotification,
                    queue = NSOperationQueue.mainQueue,
                    `object` = playerItem
                ) {
                    elapsedTime.value = Duration.ZERO
                    pause0()
                }
            }

            player.withPlayer(requireNotNull(playerItem)) { applePlayer ->
                try {
                    val startDuration = startPosition ?: elapsedTime.value ?: Duration.ZERO
                    applePlayer.seekToTime(CMTimeMake(startDuration.inWholeMilliseconds, 1000))
                    applePlayer.play()
                    player.currentItemPlaying.value = this
                } catch (ex: Exception) {
                    log.error(ex) { "Failed to play media item" }
                    itemState.value = MediaPlayer.State.Failed("Unable to play media item: ${ex.message}")
                }
            }
        }
    }

    override suspend fun pause() = player.playerMutex.withLock {
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

        seekTo0(position)
    }

    override fun close() {
        playerItem?.removeObserver(statusObserver, "status")
        playEndObserver?.let { observer ->
            NSNotificationCenter.defaultCenter.removeObserver(observer)
        }

        coroutineScope.launch {
            file.value?.delete()
        }
    }

    private fun pause0() = player.withPlayer(null) { applePlayer ->
        seekTo0(Duration.ZERO)
        applePlayer.pause()
        isPlaying.value = false
        player.currentItemPlaying.value = null
    }

    private fun seekTo0(position: Duration) = player.withPlayer(null) { player ->
        val time = CMTimeMake(position.inWholeMilliseconds, 1000)
        player.seekToTime(time)
    }

    inner class ItemStatusObserver : NSObject(), NSObjectObserverProtocol {
        override fun observeValueForKeyPath(
            keyPath: String?,
            ofObject: Any?,
            change: Map<Any?, *>?,
            context: COpaquePointer?
        ) {
            if (playerItem?.let { it.status != AVPlayerItemStatusReadyToPlay } ?: true)
                return

            playerItem?.duration?.useContents {
                if (this.timescale == 0)
                    return@useContents

                duration.value = (this.value * 1000 / this.timescale).milliseconds
            }
        }
    }
}
