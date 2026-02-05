package de.connect2x.trixnity.messenger.media

import de.connect2x.trixnity.messenger.util.toNSUrl
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import net.folivo.trixnity.client.media.PlatformMedia
import net.folivo.trixnity.client.media.okio.OkioPlatformMedia
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.AVURLAssetOverrideMIMETypeKey
import platform.AVFoundation.AVURLAssetPreferPreciseDurationAndTimingKey
import platform.AVFoundation.addPeriodicTimeObserverForInterval
import platform.AVFoundation.removeTimeObserver
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSURL
import platform.darwin.NSEC_PER_SEC
import kotlin.Any
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val log = KotlinLogging.logger { }

internal class AppleMediaPlayer : MediaPlayer {
    private var player: AVPlayer? = null
    internal val currentItemPlaying: MutableStateFlow<ApplePlayerItem?> = MutableStateFlow(null)
    internal val playerMutex: Mutex = Mutex()

    override val playingItem: StateFlow<MediaPlayer.Item?> = currentItemPlaying.asStateFlow()

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun open(
        id: String,
        media: PlatformMedia,
        mimeType: String
    ): Result<MediaPlayer.Item> {
        check(media is OkioPlatformMedia) { "PlatformMedia is required to be a OkioPlatformMedia" }
        media.getTemporaryFile().fold(
            onFailure = {
                log.error(it) { "Unable to open media as temporary file" }
                return Result.failure(it)
            },
            onSuccess = { tempFile ->
                log.debug { "Successfully opened media as temporary file" }
                try {
                    val options = mapOf<Any?, Any>(AVURLAssetOverrideMIMETypeKey to mimeType)
                    val asset = AVURLAsset.URLAssetWithURL(tempFile.path.toNSUrl(), options)
                    val duration = CMTimeGetSeconds(asset.duration).toInt()
                    if (duration == 0) {
                        return Result.failure(IllegalArgumentException("Media duration could not be extracted"))
                    }

                    return Result.success(
                        ApplePlayerItem(
                            id = id,
                            asset = asset,
                            duration = duration.seconds,
                            tempFile = tempFile,
                            coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob()),
                            player = this
                        )
                    )
                } catch (ex: Exception) {
                    return Result.failure(IllegalArgumentException("Illegal media specified", ex))
                }
            }
        )
    }

    override fun close() {
    }

    @OptIn(ExperimentalForeignApi::class)
    internal fun withPlayer(item: AVPlayerItem?, closure: (AVPlayer) -> Unit) {
        if (item != null) {
            try {
                player?.replaceCurrentItemWithPlayerItem(item)
                if (player == null) {
                    player = AVPlayer.playerWithPlayerItem(item)
                }
            } catch (ex: Exception) {
                log.error(ex) { "Unable to prepare media player" }
                return
            }
        }

        if (player != null) {
            closure(requireNotNull(player))
        }
    }
}
