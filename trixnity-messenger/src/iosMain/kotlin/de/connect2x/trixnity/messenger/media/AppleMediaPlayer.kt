package de.connect2x.trixnity.messenger.media

import de.connect2x.trixnity.messenger.interop.observer.NSObjectObserverProtocol
import de.connect2x.trixnity.messenger.util.toNSUrl
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.util.collections.ConcurrentMap
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.useContents
import kotlinx.cinterop.value
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.folivo.trixnity.client.media.PlatformMedia
import net.folivo.trixnity.client.media.okio.OkioPlatformMedia
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.AVPlayerItemStatusReadyToPlay
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.AVURLAssetOverrideMIMETypeKey
import platform.AVFoundation.addPeriodicTimeObserverForInterval
import platform.AVFoundation.duration
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.removeTimeObserver
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.AVFoundation.seekToTime
import platform.CoreMedia.CMTime
import platform.CoreMedia.CMTimeMake
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSError
import platform.Foundation.NSKeyValueObservingOptionInitial
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.addObserver
import platform.Foundation.removeObserver
import platform.darwin.NSEC_PER_SEC
import platform.darwin.NSObject
import platform.darwin.NSObjectProtocol
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private val log = KotlinLogging.logger { }

@OptIn(ExperimentalForeignApi::class)
class AppleMediaPlayer(private val coroutineScope: CoroutineScope) : MediaPlayer {
    private val playerItems: ConcurrentMap<String, PlayerItem> = ConcurrentMap()
    private val observer: AVPlayerObserver = AVPlayerObserver()
    private var tempFile: OkioPlatformMedia.TemporaryFile? = null
    private var currentPlayer: AVPlayer? = null

    override val elapsedTime: MutableStateFlow<Duration> = MutableStateFlow(Duration.ZERO)
    override val duration: MutableStateFlow<Duration> = MutableStateFlow(Duration.ZERO)
    override val isPlaying: MutableStateFlow<Boolean> = MutableStateFlow(false)

    @OptIn(BetaInteropApi::class)
    override suspend fun start(
        media: PlatformMedia,
        mimeType: String?,
        position: Duration,
        callback: (MediaPlayer.Event) -> Unit
    ) {
        check(media is OkioPlatformMedia) { "Platform media must be a OkioPlatformMedia" }
        media.getTemporaryFile().fold(
            onFailure = {
                log.error(it) { "Unexpected error while acquiring temporary file" }
            },
            onSuccess = { file ->
                log.debug { "Successfully acquired temporary file of media resource" }
                stop()

                val session = AVAudioSession.sharedInstance()
                memScoped {
                    val errorPtr = alloc<ObjCObjectVar<NSError?>>()
                    session.setCategory(AVAudioSessionCategoryPlayback, errorPtr.ptr)
                    if (errorPtr.value != null) {
                        log.error { "Unable to start audio session: ${errorPtr.value}" }
                        return@fold
                    }

                    session.setActive(true, errorPtr.ptr)
                    if (errorPtr.value != null) {
                        log.error { "Unable to start audio session: ${errorPtr.value}" }
                        return@fold
                    }
                }

                // Apple has a `playerWithURL` function allowing to specify a file itself by a file URL. This is not
                // done in this case because the path of the file doesn't end with a file extension. In such cases, it's
                // possible to use player items with URL assets allowing to specify the mime type manually and bypass
                // the automatic derivation.
                playerItems.remove(file.path.toString()) // Remove if present
                val playerItem = PlayerItem(
                    file = file,
                    coroutineScope = coroutineScope,
                    item = AVPlayerItem.playerItemWithAsset(AVURLAsset.URLAssetWithURL(
                        file.path.toNSUrl(),
                        mapOf(AVURLAssetOverrideMIMETypeKey to mimeType!!) // TODO: Alternative when null
                    )),
                    sendEvent = callback,
                    onReadyForPlay = { duration ->
                        this.duration.value = duration
                    },
                    onClose = {
                        playerItems.remove(file.path.toString())
                    }
                )
                playerItems[file.path.toString()] = playerItem

                currentPlayer?.replaceCurrentItemWithPlayerItem(playerItem.item)
                if (currentPlayer == null) {
                    currentPlayer = AVPlayer.playerWithPlayerItem(playerItem.item)
                    observer.attach()
                }

                currentPlayer?.seekToTime(CMTimeMake(position.inWholeMilliseconds, 1000))
                isPlaying.value = true
                currentPlayer?.play()
                tempFile = file
            }
        )
    }

    override suspend fun stop() {
        elapsedTime.value = Duration.ZERO
        duration.value = Duration.ZERO
        isPlaying.value = false
        if (currentPlayer != null) {
            currentPlayer?.pause()
        }

        if (tempFile != null) {
            tempFile?.delete()
            tempFile = null
        }
    }

    override suspend fun seekTo(position: Duration) {
        currentPlayer?.seekToTime(CMTimeMake(position.inWholeMilliseconds, 1000))
        elapsedTime.value = position
    }

    override fun close(): Unit = runBlocking {
        playerItems.forEach { (_, item) -> item.close() }
        playerItems.clear()
        stop()
        observer.detach()
    }

    class PlayerItem(
        val item: AVPlayerItem,
        private val coroutineScope: CoroutineScope,
        private val file: OkioPlatformMedia.TemporaryFile,
        private val sendEvent: (MediaPlayer.Event) -> Unit,
        private val onReadyForPlay: (Duration) -> Unit,
        private val onClose: () -> Unit
    ) : NSObject(), NSObjectObserverProtocol {
        private val playbackEndObserver: NSObjectProtocol = NSNotificationCenter.defaultCenter.addObserverForName(
            name = AVPlayerItemDidPlayToEndTimeNotification,
            queue = NSOperationQueue.mainQueue,
            `object` = item
        ) {
            log.debug { "Playback of media item has been finished, stopping playback..." }
            sendEvent(MediaPlayer.Event.Progress(elapsedTime = Duration.ZERO, duration = null))
            close()
        }

        init {
            item.addObserver(this, "status", NSKeyValueObservingOptionInitial, null)
        }

        fun close() {
            NSNotificationCenter.defaultCenter.removeObserver(playbackEndObserver)
            item.removeObserver(this, "status")
            sendEvent(MediaPlayer.Event.Stopped)
            onClose()
            coroutineScope.launch {
                file.delete()
            }
        }

        override fun observeValueForKeyPath(
            keyPath: String?,
            ofObject: Any?,
            change: Map<Any?, *>?,
            context: COpaquePointer?
        ) {
            if (item.status == AVPlayerItemStatusReadyToPlay) {
                log.trace { "Player item is ready to play, applying duration" }
                item.duration.useContents {
                    if (this.timescale == 0)
                        return@useContents

                    onReadyForPlay((this.value * 1000 / this.timescale).milliseconds)
                }
            }
        }
    }

    /**
     * This observer is used to listen for time changes while playing a media file. So it updates the elapsed time info
     * of the media player. It also notifies the media player when it has finished the playback of the media file.
     */
    inner class AVPlayerObserver {
        private var timeObserver: Any? = null

        fun attach() {
            detach()
            currentPlayer?.let { player ->
                val interval = CMTimeMakeWithSeconds(0.25, NSEC_PER_SEC.toInt()) // 250ms
                timeObserver = player.addPeriodicTimeObserverForInterval(interval, null) { time: CValue<CMTime> ->
                    if (!isPlaying.value) {
                        return@addPeriodicTimeObserverForInterval
                    }

                    time.useContents {
                        if (timescale > 0) {
                            elapsedTime.value = (this.value * 1000 / this.timescale).milliseconds
                        }
                    }
                }
            }
        }

        fun detach() = timeObserver?.let {
            currentPlayer?.removeTimeObserver(it)
        }
    }
}
