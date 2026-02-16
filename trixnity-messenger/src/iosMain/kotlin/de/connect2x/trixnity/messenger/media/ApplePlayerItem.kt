package de.connect2x.trixnity.messenger.media

import de.connect2x.lognity.api.logger.error
import de.connect2x.trixnity.client.media.okio.OkioPlatformMedia
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
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
    private val player: AppleMediaPlayer,
) : AbstractMediaItem(coroutineScope, player.playerMutex, player.currentItemPlaying) {
    private var playerItem: AVPlayerItem = AVPlayerItem.playerItemWithAsset(asset)
    private var playEndObserver: NSObjectProtocol? = null
    private var timeObserver: Any? = null

    init {
        playEndObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            name = AVPlayerItemDidPlayToEndTimeNotification,
            queue = NSOperationQueue.mainQueue,
            `object` = playerItem
        ) {
            elapsedTime.value = Duration.ZERO
            onPauseNotBlocking()
        }
    }

    override suspend fun onPlay(duration: Duration): Result<Unit> = player.withPlayer(playerItem) { applePlayer ->
        try {
            applePlayer.seekToTime(CMTimeMake(duration.inWholeMilliseconds, 1000))
            applePlayer.play()

            val interval = CMTimeMakeWithSeconds(0.125, NSEC_PER_SEC.toInt()) // 125ms
            timeObserver = applePlayer.addPeriodicTimeObserverForInterval(interval, null) { time ->
                time.useContents {
                    if (timescale <= 0 || state.value !is MediaPlayer.Item.State.Playing)
                        return@useContents

                    elapsedTime.value = (this.value * 1000 / timescale).milliseconds
                }
            }

            player.currentItemPlaying.value = this
            state.value = MediaPlayer.Item.State.Playing
            Result.success(Unit)
        } catch (ex: Exception) {
            log.error(ex) { "Failed to play media item" }
            Result.failure(Exception("Unable to play media item: ${ex.message}"))
        }
    } ?: Result.success(Unit)

    override suspend fun onPause() = onPauseNotBlocking()
    override suspend fun onSeekTo(position: Duration) = onSeekToNotBlocking(position)

    override fun close() {
        playEndObserver?.let { observer ->
            NSNotificationCenter.defaultCenter.removeObserver(observer)
        }

        coroutineScope.launch {
            if (state.value is MediaPlayer.Item.State.Playing) {
                pauseWithoutLock()
            }

            tempFile.delete()
        }
    }

    private fun onPauseNotBlocking() {
        onSeekToNotBlocking(Duration.ZERO)
        player.withPlayer(null) { applePlayer ->
            applePlayer.pause()
            timeObserver?.let {
                applePlayer.removeTimeObserver(it)
                timeObserver = null
            }
        }
    }

    private fun onSeekToNotBlocking(position: Duration) {
        player.withPlayer(null) { player ->
            val time = CMTimeMake(position.inWholeMilliseconds, 1000)
            player.seekToTime(time)
        }
    }
}
