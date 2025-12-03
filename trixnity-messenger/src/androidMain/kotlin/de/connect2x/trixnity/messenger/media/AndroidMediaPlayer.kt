package de.connect2x.trixnity.messenger.media

import android.content.ComponentName
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import de.connect2x.trixnity.messenger.util.ContextGetter
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.folivo.trixnity.client.media.PlatformMedia
import net.folivo.trixnity.client.media.okio.OkioPlatformMedia
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class AndroidMediaPlayer(getContext: ContextGetter) : MediaPlayer {
    private var controller: MediaController? = null

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var updateJob: Job? = null

    override val elapsedTime: MutableStateFlow<Duration> = MutableStateFlow(Duration.ZERO)
    override val duration: MutableStateFlow<Duration> = MutableStateFlow(Duration.ZERO)
    override val isPlaying: MutableStateFlow<Boolean> = MutableStateFlow(false)

    init {
        val context = getContext()
        val sessionToken = SessionToken(context, ComponentName(context, AudioPlayerService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        val futureListener = Runnable {
            controller = controllerFuture.get()
            controller?.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    val controller = requireNotNull(controller)
                    if (controller.playbackState == Player.STATE_READY) {
                        elapsedTime.value = controller.currentPosition.coerceAtLeast(0).milliseconds
                        duration.value = controller.duration.coerceAtLeast(0).milliseconds
                    }

                    if (!isPlaying) {
                        updateJob?.cancel()
                        duration.value = Duration.ZERO
                        this@AndroidMediaPlayer.isPlaying.value = false
                        runBlocking {
                            val localConfiguration = requireNotNull(controller.currentMediaItem?.localConfiguration)
                            (localConfiguration.tag as OkioPlatformMedia.TemporaryFile?)?.delete()
                        }
                    }
                }
            })
        }

        controllerFuture.addListener(futureListener, ContextCompat.getMainExecutor(context))
    }

    override suspend fun start(media: PlatformMedia, mimeType: String?, position: Duration) { // TODO: We want a callback for playback stop
        require(media is OkioPlatformMedia) { "Media is expected to be a OkioPlatformMedia" }
        val tempFile = media.getTemporaryFile().getOrThrow()

        val controller = checkNotNull(controller) { "Unable to play media when media player is not available yet!" }
        withContext(Dispatchers.Main) {
            if (isPlaying.value) {
                stop()
            }

            controller.setMediaItem(MediaItem.Builder()
                .setMediaId(tempFile.path.toString())
                .setTag(tempFile)
                .setMimeType(mimeType ?: MimeTypes.AUDIO_RAW).build())
            controller.prepare()
            controller.play()
            controller.seekTo(position.inWholeMilliseconds)
            isPlaying.value = true

            updateJob = coroutineScope.launch(CoroutineName("Media Update Job")) {
                while (isActive && isPlaying.value) {
                    elapsedTime.value = controller.currentPosition.coerceAtLeast(0).milliseconds
                    duration.value = controller.duration.coerceAtLeast(0).milliseconds
                    delay(100.milliseconds)
                }
            }
        }
    }

    override suspend fun stop() {
        check(isPlaying.value) { "Unable to stop media playback without anything being played" }
        withContext(Dispatchers.Main) {
            controller?.stop()
            updateJob?.cancel()
            isPlaying.value = false
        }
    }

    override suspend fun seekTo(position: Duration) {
        check(isPlaying.value) { "Unable to seek playback position without anything being played" }
        withContext(Dispatchers.Main) {
            controller?.seekTo(position.inWholeMilliseconds)
        }
    }

    override fun close() {
        updateJob?.cancel()
        coroutineScope.cancel()
    }


}
