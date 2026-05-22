package de.connect2x.trixnity.messenger.media

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.lognity.api.logger.error
import de.connect2x.trixnity.client.media.PlatformMedia
import de.connect2x.trixnity.client.media.okio.OkioPlatformMedia
import de.connect2x.trixnity.messenger.util.toNSUrl
import kotlin.Any
import kotlin.time.Duration.Companion.seconds
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.AVURLAssetOverrideMIMETypeKey
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.CoreMedia.CMTimeGetSeconds

internal class AppleMediaPlayer(private val coroutineScope: CoroutineScope) : MediaPlayer {
    private val log: Logger = Logger("de.connect2x.trixnity.messenger.media.AppleMediaPlayer")
    private var player: AVPlayer? = null
    internal val currentItemPlaying: MutableStateFlow<AbstractMediaItem?> = MutableStateFlow(null)
    internal val playerMutex: Mutex = Mutex()

    override val playingItem: StateFlow<MediaPlayer.Item?> = currentItemPlaying.asStateFlow()

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun open(
        id: String,
        media: PlatformMedia,
        mimeType: String,
        lifecycleScope: CoroutineScope?,
    ): Result<MediaPlayer.Item> {
        check(media is OkioPlatformMedia) { "PlatformMedia is required to be a OkioPlatformMedia" }
        val playingItem = playingItem.value
        if (playingItem != null && playingItem.id == id) {
            playingItem.updateLifecycle(lifecycleScope)
            return Result.success(playingItem)
        }

        media
            .getTemporaryFile()
            .fold(
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

                        val coroutineCtx = coroutineScope.coroutineContext
                        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
                            log.error(throwable) { "Unexpected error while running media player" }
                        }

                        val scope = CoroutineScope(coroutineCtx + SupervisorJob(coroutineCtx[Job]) + exceptionHandler)
                        val mediaItem =
                            ApplePlayerItem(
                                id = id,
                                asset = asset,
                                duration = duration.seconds,
                                tempFile = tempFile,
                                coroutineScope = scope,
                                player = this,
                            )

                        mediaItem.updateLifecycle(lifecycleScope)
                        return Result.success(mediaItem)
                    } catch (ex: Exception) {
                        return Result.failure(IllegalArgumentException("Illegal media specified", ex))
                    }
                },
            )
    }

    override fun close() {}

    @OptIn(ExperimentalForeignApi::class)
    internal fun <R> withPlayer(item: AVPlayerItem?, closure: (AVPlayer) -> R): R? {
        if (item != null) {
            try {
                player?.replaceCurrentItemWithPlayerItem(item)
                if (player == null) {
                    player = AVPlayer.playerWithPlayerItem(item)
                }
            } catch (ex: Exception) {
                log.error(ex) { "Unable to prepare media player" }
                return null
            }
        }

        if (player != null) {
            return closure(requireNotNull(player))
        }

        return null
    }
}
