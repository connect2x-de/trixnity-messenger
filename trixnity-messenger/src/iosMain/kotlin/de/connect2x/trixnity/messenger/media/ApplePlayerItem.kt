package de.connect2x.trixnity.messenger.media

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.lognity.api.logger.error
import de.connect2x.trixnity.client.media.okio.OkioPlatformMedia
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.addPeriodicTimeObserverForInterval
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.removeTimeObserver
import platform.AVFoundation.seekToTime
import platform.CoreMedia.CMTimeMake
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.darwin.NSEC_PER_SEC
import platform.darwin.NSObjectProtocol
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalForeignApi::class)
internal class ApplePlayerItem(
    override val id: String,
    override val duration: Duration,
    asset: AVURLAsset,
    private val tempFile: OkioPlatformMedia.TemporaryFile,
    private val coroutineScope: CoroutineScope,
    lifecycleScope: CoroutineScope?,
    private val player: AppleMediaPlayer,
    override val state: MutableStateFlow<MediaPlayer.State> = MutableStateFlow(MediaPlayer.State.Ready)
) : MediaLifecycleItemImpl(coroutineScope, state), MediaPlayer.Item {
    private val log: Logger = Logger("de.connect2x.trixnity.messenger.media.AppleMediaItem")
    private var playEndObserver: NSObjectProtocol? = null
    private var timeObserver: Any? = null
    private var playerItem: AVPlayerItem? = null

    override val elapsedTime: MutableStateFlow<Duration?> = MutableStateFlow(null)

    init {
        updateLifecycle(lifecycleScope)
        playerItem = AVPlayerItem.playerItemWithAsset(asset)
        playEndObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            name = AVPlayerItemDidPlayToEndTimeNotification,
            queue = NSOperationQueue.mainQueue,
            `object` = playerItem
        ) {
            elapsedTime.value = Duration.ZERO
            pause0()
        }
    }

    override suspend fun play() {
        player.playerMutex.withLock {
            if (state.value !is MediaPlayer.State.Ready) {
                log.error { "Unable to start playback: Media file is not in the state to be played" }
                return
            }

            log.debug { "Stop previously played media file before starting to play new media file" }
            player.currentItemPlaying.value?.pause0() // Stop the currently playing element if one is currently played

            log.debug { "Starting playback of media item" }
            player.withPlayer(requireNotNull(playerItem)) { applePlayer ->
                try {
                    val startDuration = elapsedTime.value ?: Duration.ZERO
                    applePlayer.seekToTime(CMTimeMake(startDuration.inWholeMilliseconds, 1000))
                    applePlayer.play()

                    val interval = CMTimeMakeWithSeconds(0.125, NSEC_PER_SEC.toInt()) // 125ms
                    timeObserver = applePlayer.addPeriodicTimeObserverForInterval(interval, null) { time ->
                        time.useContents {
                            if (timescale <= 0 || state.value !is MediaPlayer.State.Playing)
                                return@useContents

                            elapsedTime.value = (this.value * 1000 / timescale).milliseconds
                        }
                    }

                    player.currentItemPlaying.value = this
                    state.value = MediaPlayer.State.Playing
                } catch (ex: Exception) {
                    log.error(ex) { "Failed to play media item" }
                    state.value = MediaPlayer.State.Failed("Unable to play media item: ${ex.message}")
                }
            }
        }
    }

    override suspend fun pause() = player.playerMutex.withLock {
        if (state.value !is MediaPlayer.State.Playing) {
            log.error { "Unable to stop playback: Media file is not in the state to be stopped" }
            return@withLock
        }

        pause0()
    }

    override suspend fun seekTo(position: Duration) {
        if (state.value !is MediaPlayer.State.Playing) {
            elapsedTime.value = position
            return
        }

        seekTo0(position)
    }

    override fun close() {
        if (state.value is MediaPlayer.State.Playing) {
            pause0()
        }

        playEndObserver?.let { observer ->
            NSNotificationCenter.defaultCenter.removeObserver(observer)
        }

        coroutineScope.launch {
            tempFile.delete()
        }
    }

    private fun pause0() = player.withPlayer(null) { applePlayer ->
        seekTo0(Duration.ZERO)
        applePlayer.pause()
        player.currentItemPlaying.value = null
        state.value = MediaPlayer.State.Ready
        timeObserver?.let {
            applePlayer.removeTimeObserver(it)
            timeObserver = null
        }
    }

    private fun seekTo0(position: Duration) = player.withPlayer(null) { player ->
        val time = CMTimeMake(position.inWholeMilliseconds, 1000)
        player.seekToTime(time)
    }
}
