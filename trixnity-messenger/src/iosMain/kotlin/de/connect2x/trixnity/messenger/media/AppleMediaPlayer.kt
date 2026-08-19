package de.connect2x.trixnity.messenger.media

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.lognity.api.logger.error
import de.connect2x.trixnity.client.media.PlatformMedia
import de.connect2x.trixnity.client.media.okio.OkioPlatformMedia
import de.connect2x.trixnity.messenger.util.toNSUrl
import kotlin.Any
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.AVURLAssetOverrideMIMETypeKey
import platform.AVFoundation.pause
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.CoreMedia.CMTimeGetSeconds
import platform.MediaPlayer.MPChangePlaybackPositionCommandEvent
import platform.MediaPlayer.MPMediaItemPropertyPlaybackDuration
import platform.MediaPlayer.MPMediaItemPropertyTitle
import platform.MediaPlayer.MPNowPlayingInfoCenter
import platform.MediaPlayer.MPNowPlayingInfoPropertyElapsedPlaybackTime
import platform.MediaPlayer.MPNowPlayingInfoPropertyExternalContentIdentifier
import platform.MediaPlayer.MPNowPlayingInfoPropertyPlaybackRate
import platform.MediaPlayer.MPRemoteCommand
import platform.MediaPlayer.MPRemoteCommandCenter
import platform.MediaPlayer.MPRemoteCommandHandlerStatusCommandFailed
import platform.MediaPlayer.MPRemoteCommandHandlerStatusNoActionableNowPlayingItem
import platform.MediaPlayer.MPRemoteCommandHandlerStatusSuccess

@OptIn(ExperimentalForeignApi::class)
internal class AppleMediaPlayer(private val coroutineScope: CoroutineScope) : MediaPlayer {
    private val log: Logger = Logger("de.connect2x.trixnity.messenger.media.AppleMediaPlayer")
    private var player: AVPlayer? = null
    internal val currentItemPlaying: MutableStateFlow<ApplePlayerItem?> = MutableStateFlow(null)
    private var nowPlayingItem: ApplePlayerItem? = null
    private var isClosed: Boolean = false
    private val remoteCommandTargets: List<Pair<MPRemoteCommand, Any>>
    internal val playerMutex: Mutex = Mutex()

    override val playingItem: StateFlow<MediaPlayer.Item?> = currentItemPlaying.asStateFlow()

    init {
        val commandCenter = MPRemoteCommandCenter.sharedCommandCenter()
        remoteCommandTargets =
            listOf(
                commandCenter.playCommand to
                    commandCenter.playCommand.addTargetWithHandler {
                        val item =
                            nowPlayingItem
                                ?: return@addTargetWithHandler MPRemoteCommandHandlerStatusNoActionableNowPlayingItem
                        when (item.state.value) {
                            is MediaPlayer.Item.State.Ready -> coroutineScope.launch { item.play() }
                            is MediaPlayer.Item.State.Playing -> Unit
                            is MediaPlayer.Item.State.Failed ->
                                return@addTargetWithHandler MPRemoteCommandHandlerStatusCommandFailed
                        }
                        MPRemoteCommandHandlerStatusSuccess
                    },
                commandCenter.pauseCommand to
                    commandCenter.pauseCommand.addTargetWithHandler {
                        val item =
                            nowPlayingItem
                                ?: return@addTargetWithHandler MPRemoteCommandHandlerStatusNoActionableNowPlayingItem
                        if (item.state.value is MediaPlayer.Item.State.Playing) {
                            coroutineScope.launch { item.pause() }
                        }
                        MPRemoteCommandHandlerStatusSuccess
                    },
                commandCenter.togglePlayPauseCommand to
                    commandCenter.togglePlayPauseCommand.addTargetWithHandler {
                        val item =
                            nowPlayingItem
                                ?: return@addTargetWithHandler MPRemoteCommandHandlerStatusNoActionableNowPlayingItem
                        when (item.state.value) {
                            is MediaPlayer.Item.State.Ready -> coroutineScope.launch { item.play() }
                            is MediaPlayer.Item.State.Playing -> coroutineScope.launch { item.pause() }
                            is MediaPlayer.Item.State.Failed ->
                                return@addTargetWithHandler MPRemoteCommandHandlerStatusCommandFailed
                        }
                        MPRemoteCommandHandlerStatusSuccess
                    },
                commandCenter.changePlaybackPositionCommand to
                    commandCenter.changePlaybackPositionCommand.addTargetWithHandler { event ->
                        val item =
                            nowPlayingItem
                                ?: return@addTargetWithHandler MPRemoteCommandHandlerStatusNoActionableNowPlayingItem
                        val position =
                            (event as? MPChangePlaybackPositionCommandEvent)?.positionTime
                                ?: return@addTargetWithHandler MPRemoteCommandHandlerStatusCommandFailed
                        if (!position.isFinite() || position < 0) {
                            return@addTargetWithHandler MPRemoteCommandHandlerStatusCommandFailed
                        }
                        val targetPosition = position.seconds.coerceAtMost(item.duration)
                        coroutineScope.launch { item.seekTo(targetPosition) }
                        MPRemoteCommandHandlerStatusSuccess
                    },
            )
    }

    override suspend fun open(
        id: String,
        media: PlatformMedia,
        mimeType: String,
        lifecycleScope: CoroutineScope?,
    ): Result<MediaPlayer.Item> {
        configureSession().onFailure {
            log.error(it) { "Unable to configure audio session" }
            return Result.failure(it)
        }

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

                        val mediaItem =
                            ApplePlayerItem(
                                id = id,
                                displayName = tempFile.path.name,
                                asset = asset,
                                duration = duration.seconds,
                                tempFile = tempFile,
                                coroutineScope = coroutineScope,
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

    override fun close() {
        if (isClosed) return

        isClosed = true
        remoteCommandTargets.forEach { (command, target) -> command.removeTarget(target) }
        player?.pause()
        player?.replaceCurrentItemWithPlayerItem(null)
        player = null
        nowPlayingItem = null
        MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = null
    }

    internal fun isCurrentItem(item: ApplePlayerItem): Boolean = currentItemPlaying.value === item

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

    internal fun updateNowPlayingInfo(item: ApplePlayerItem, elapsedTime: Duration, isPlaying: Boolean) {
        if (isClosed) return

        nowPlayingItem = item
        MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo =
            mapOf(
                MPMediaItemPropertyTitle to item.displayName,
                MPMediaItemPropertyPlaybackDuration to item.duration.inWholeMilliseconds / 1000.0,
                MPNowPlayingInfoPropertyElapsedPlaybackTime to elapsedTime.inWholeMilliseconds / 1000.0,
                MPNowPlayingInfoPropertyPlaybackRate to if (isPlaying) 1.0 else 0.0,
                MPNowPlayingInfoPropertyExternalContentIdentifier to item.id,
            )
    }

    internal fun refreshNowPlayingInfo(item: ApplePlayerItem, elapsedTime: Duration) {
        if (nowPlayingItem != item) return

        updateNowPlayingInfo(
            item = item,
            elapsedTime = elapsedTime,
            isPlaying = item.state.value is MediaPlayer.Item.State.Playing,
        )
    }

    internal fun clearNowPlayingInfo(item: ApplePlayerItem) {
        if (nowPlayingItem != item) return

        nowPlayingItem = null
        MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = null
    }
}
