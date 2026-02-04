package de.connect2x.trixnity.messenger.media

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import net.folivo.trixnity.client.media.PlatformMedia
import net.folivo.trixnity.client.media.okio.OkioPlatformMedia
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.addPeriodicTimeObserverForInterval
import platform.AVFoundation.removeTimeObserver
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.darwin.NSEC_PER_SEC
import kotlin.time.Duration.Companion.milliseconds

private val log = KotlinLogging.logger { }

internal class AppleMediaPlayer : MediaPlayer {
    private var player: AVPlayer? = null
    private var timeObserver: Any? = null

    internal val currentItemPlaying: MutableStateFlow<ApplePlayerItem?> = MutableStateFlow(null)
    internal val playerMutex: Mutex = Mutex()

    override val state: MutableStateFlow<MediaPlayer.State> = MutableStateFlow(MediaPlayer.State.Ready)
    override val playingItem: StateFlow<MediaPlayer.Item?> = currentItemPlaying.asStateFlow()

    override suspend fun open(
        media: PlatformMedia,
        mimeType: String,
        coroutineScope: CoroutineScope
    ): Result<MediaPlayer.Item> {
        check(media is OkioPlatformMedia) { "PlatformMedia is required to be a OkioPlatformMedia" }
        media.getTemporaryFile().fold(
            onFailure = {
                log.error(it) { "Unable to open media as temporary file" }
                return Result.failure(it)
            },
            onSuccess = { tempFile ->
                log.debug { "Successfully opened media as temporary file" }
                return Result.success(ApplePlayerItem(tempFile, mimeType, coroutineScope, this))
            }
        )
    }

    override fun close() {
        timeObserver?.let {
            player?.removeTimeObserver(it)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    internal fun withPlayer(item: AVPlayerItem?, closure: (AVPlayer) -> Unit) {
        if (item != null) {
            try {
                player?.replaceCurrentItemWithPlayerItem(item)
                if (player == null) {
                    player = AVPlayer.playerWithPlayerItem(item)

                    val interval = CMTimeMakeWithSeconds(0.125, NSEC_PER_SEC.toInt()) // 125ms
                    timeObserver = player?.addPeriodicTimeObserverForInterval(interval, null) { time ->
                        time.useContents {
                            if (timescale <= 0)
                                return@useContents

                            val elapsedTime = (this.value * 1000 / this.timescale).milliseconds
                            currentItemPlaying.value?.elapsedTime?.value = elapsedTime
                        }
                    }
                }
            } catch (ex: Exception) {
                log.error(ex) { "Unable to prepare media player" }
                state.value = MediaPlayer.State.Failed("Unable to prepare media player: ${ex.message}")
            }
        }

        if (player != null) {
            closure(requireNotNull(player))
        }
    }
}
