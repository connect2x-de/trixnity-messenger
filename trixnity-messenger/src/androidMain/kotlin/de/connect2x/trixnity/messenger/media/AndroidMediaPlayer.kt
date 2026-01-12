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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.folivo.trixnity.client.media.PlatformMedia
import net.folivo.trixnity.client.media.okio.OkioPlatformMedia
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private val log = KotlinLogging.logger { }

class AndroidMediaPlayer(getContext: ContextGetter, private val coroutineScope: CoroutineScope) : MediaPlayer {
    private var controller: MediaController? = null
    private val mediaDataStore: MutableMap<String, Playback> = ConcurrentHashMap()

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
                    val metadata = mediaDataStore[controller.currentMediaItem?.mediaId]
                    if (controller.playbackState == Player.STATE_READY) {
                        val elapsedTime = controller.currentPosition.coerceAtLeast(0).milliseconds
                        val duration = controller.duration.coerceAtLeast(0).milliseconds
                        metadata?.callback(MediaPlayer.Event.Progress(elapsedTime, duration))
                        this@AndroidMediaPlayer.elapsedTime.value = elapsedTime
                        this@AndroidMediaPlayer.duration.value = duration
                    }

                    if (!isPlaying) {
                        metadata?.let {
                            it.close()
                            mediaDataStore.remove(controller.currentMediaItem?.mediaId)
                        }
                        duration.value = Duration.ZERO
                        this@AndroidMediaPlayer.isPlaying.value = false
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
            elapsedTime.value = Duration.ZERO
            duration.value = Duration.ZERO

            log.trace { "The media requested is not dame media as played last time, queueing new media" }
            stop0(pause = false)

            media.getTemporaryFile().fold(
                onFailure = {
                    log.error(it) { "Unexpected error when acquiring file for playback" }
                },
                onSuccess = { tempFile ->
                    log.info { "Successfully acquired file, starting playback" }
                    isPlaying.value = true
                    mediaDataStore[tempFile.path.toString()] = Playback(
                        callback = callback,
                        updateJob = coroutineScope.launch {
                            while (isActive && isPlaying.value) {
                                withContext(Dispatchers.Main) {
                                    val elapsedTime = controller.currentPosition.coerceAtLeast(0).milliseconds
                                    val duration = controller.duration.coerceAtLeast(0).milliseconds
                                    callback(MediaPlayer.Event.Progress(elapsedTime, duration))
                                    this@AndroidMediaPlayer.elapsedTime.value = elapsedTime
                                    this@AndroidMediaPlayer.duration.value = duration
                                }
                                delay(150)
                            }
                        }
                    )

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

    override suspend fun stop() = stop0(pause = true)

    override suspend fun seekTo(position: Duration) {
        if (!isPlaying.value) {
            log.error { "Unable to seek media playback because player is currently not playing" }
            return
        }

        withContext(Dispatchers.Main) {
            if (controller?.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)?.not() ?: true) {
                log.error { "Unable to seek media playback because seek command is not available" }
                return@withContext
            }

            log.debug { "Seeking media playback to $position" }
            controller?.seekTo(position.inWholeMilliseconds)
        }
    }

    override fun close() {
        mediaDataStore.forEach { (_, entry) -> entry.close() }
        mediaDataStore.clear()
    }

    suspend fun stop0(pause: Boolean) {
        withContext(Dispatchers.Main) {
            val mediaId = controller?.currentMediaItem?.mediaId
            if (pause) {
                mediaId?.let { mediaDataStore.remove(mediaId)?.pause() }
                controller?.pause()
            } else {
                mediaId?.let { mediaDataStore.remove(mediaId)?.close() }
                controller?.stop()
            }

            isPlaying.value = false
        }
    }

    data class Playback(
        val callback: (MediaPlayer.Event) -> Unit,
        val updateJob: Job,
        var isClosed: Boolean = false
    ) : AutoCloseable {
        fun pause() {
            callback(MediaPlayer.Event.Stopped)
            updateJob.cancel()
        }

        override fun close() {
            log.debug { "Closing playback resource by sending stop event and cancel update job" }
            callback(MediaPlayer.Event.Progress(Duration.ZERO, Duration.ZERO))
            pause()
        }
    }
}
