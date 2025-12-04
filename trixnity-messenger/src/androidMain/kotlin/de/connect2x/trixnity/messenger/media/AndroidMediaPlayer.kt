package de.connect2x.trixnity.messenger.media

import android.content.ComponentName
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import de.connect2x.trixnity.messenger.util.ContextGetter
import io.github.oshai.kotlinlogging.KotlinLogging
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
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private val log = KotlinLogging.logger { }

class AndroidMediaPlayer(getContext: ContextGetter) : MediaPlayer {
    private var controller: MediaController? = null

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val lastPlayedMedia: MutableStateFlow<PlatformMedia?> = MutableStateFlow(null)
    private val mediaDataStore: MutableMap<String, CallbackInfo> = ConcurrentHashMap()
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
                        mediaDataStore.remove(controller.currentMediaItem?.mediaId)?.let { callbackInfo ->
                            log.debug { "Found media callback, informing user with event and delete temp file" }
                            callbackInfo.callback(MediaPlayer.Event.Stopped)
                            runBlocking {
                                callbackInfo.tempFile.delete()
                            }
                        }
                    }
                }
            })
        }

        controllerFuture.addListener(futureListener, ContextCompat.getMainExecutor(context))
    }

    override suspend fun start(
        media: PlatformMedia,
        mimeType: String?,
        position: Duration,
        callback: (MediaPlayer.Event) -> Unit
    ) {
        require(media is OkioPlatformMedia) { "Media is expected to be a OkioPlatformMedia" }
        val controller = checkNotNull(controller) { "Unable to play media when media player is not available yet!" }
        withContext(Dispatchers.Main) {
            when (lastPlayedMedia.value == media) {
                true -> {
                    log.trace { "The media requested is the same media as played last time, unpausing playback" }
                    if (!isPlaying.value) {
                        controller.play()
                    }
                }
                false -> {
                    log.trace { "The media requested is not dame media as played last time, queueing new media" }
                    if (isPlaying.value) {
                        controller.stop()
                        updateJob?.cancel()
                    }

                    media.getTemporaryFile().fold(
                        onFailure = {
                            log.error(it) { "Unexpected error when acquiring file for playback" }
                            return@withContext
                        },
                        onSuccess = { tempFile ->
                            mediaDataStore[tempFile.path.toString()] = CallbackInfo(tempFile, callback)
                            controller.setMediaItem(MediaItem.Builder()
                                .setMediaId(tempFile.path.toString())
                                .setMimeType(mimeType ?: MimeTypes.AUDIO_RAW).build())
                            controller.prepare()
                            controller.seekTo(position.inWholeMilliseconds)
                            controller.play()
                        }
                    )
                }
            }

            lastPlayedMedia.value = media
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
        if (!isPlaying.value) {
            log.warn { "Playback is not being stopped because no playback is currently running" }
            return
        }

        withContext(Dispatchers.Main) {
            controller?.pause()
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

    data class CallbackInfo(
        val tempFile: OkioPlatformMedia.TemporaryFile,
        val callback: (MediaPlayer.Event) -> Unit
    )
}
