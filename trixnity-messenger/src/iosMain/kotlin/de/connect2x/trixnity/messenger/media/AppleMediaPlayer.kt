package de.connect2x.trixnity.messenger.media

import de.connect2x.trixnity.messenger.util.toByteArray
import de.connect2x.trixnity.messenger.util.toNSUrl
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.media.PlatformMedia
import net.folivo.trixnity.client.media.okio.OkioPlatformMedia
import okio.FileSystem
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.AVURLAssetOverrideMIMETypeKey
import platform.AVFoundation.addPeriodicTimeObserverForInterval
import platform.AVFoundation.currentItem
import platform.AVFoundation.isMuted
import platform.AVFoundation.play
import platform.AVFoundation.seekToTime
import platform.AVFoundation.volume
import platform.CoreMedia.CMTimeMake
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.Foundation.addObserver
import platform.Foundation.create
import platform.darwin.NSEC_PER_SEC
import kotlin.time.Duration

private val log = KotlinLogging.logger { }

@OptIn(ExperimentalForeignApi::class)
class AppleMediaPlayer : MediaPlayer {
    private var currentPlayer: AVPlayer? = null

    override val elapsedTime: StateFlow<Duration> = MutableStateFlow(Duration.ZERO)
    override val duration: StateFlow<Duration> = MutableStateFlow(Duration.ZERO)
    override val isPlaying: StateFlow<Boolean> = MutableStateFlow(false)

    @OptIn(BetaInteropApi::class)
    override suspend fun start(
        media: PlatformMedia,
        mimeType: String?,
        position: Duration,
        eventCallback: (MediaPlayer.Event) -> Unit
    ) {
        check(media is OkioPlatformMedia) { "Platform media must be a OkioPlatformMedia" }
        media.getTemporaryFile().fold(
            onFailure = {
                log.error(it) { "Unexpected error while acquiring temporary file" }
            },
            onSuccess = { file ->
                log.debug { "Successfully acquired temporary file of media resource" }
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

                try {
                    val playerItem = AVPlayerItem.playerItemWithAsset(AVURLAsset.URLAssetWithURL(
                        file.path.toNSUrl(),
                        mapOf(AVURLAssetOverrideMIMETypeKey to mimeType!!)
                    ))
                    
                    currentPlayer = AVPlayer.playerWithPlayerItem(playerItem)
                    currentPlayer?.seekToTime(CMTimeMake(position.inWholeMilliseconds, 1000))
                    currentPlayer?.play()
                } catch (e: Exception) {
                    log.error(e) { "Error while playing media" }
                }

            }
        )
    }

    override suspend fun stop() {
        TODO("Not yet implemented")
    }

    override suspend fun seekTo(position: Duration) {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }

}
