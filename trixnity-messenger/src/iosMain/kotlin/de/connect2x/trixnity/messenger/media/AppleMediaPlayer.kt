package de.connect2x.trixnity.messenger.media

import de.connect2x.trixnity.messenger.util.toNSUrl
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.folivo.trixnity.client.media.PlatformMedia
import net.folivo.trixnity.client.media.okio.OkioPlatformMedia
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.play
import platform.AVFoundation.seekToTime
import platform.CoreMedia.CMTimeMake
import kotlin.time.Duration

private val log = KotlinLogging.logger { }

@OptIn(ExperimentalForeignApi::class)
class AppleMediaPlayer : MediaPlayer {
    private var currentPlayer: AVPlayer? = null

    override val elapsedTime: StateFlow<Duration> = MutableStateFlow(Duration.ZERO)
    override val duration: StateFlow<Duration> = MutableStateFlow(Duration.ZERO)
    override val isPlaying: StateFlow<Boolean> = MutableStateFlow(false)

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
                currentPlayer = AVPlayer.playerWithURL(file.path.toNSUrl())
                currentPlayer?.seekToTime(CMTimeMake(position.inWholeMilliseconds, 1000))
                currentPlayer?.play()
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
