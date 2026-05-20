package de.connect2x.trixnity.messenger.media

import de.connect2x.trixnity.client.media.PlatformMedia
import de.connect2x.trixnity.messenger.util.handleFirst
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import web.events.ENDED
import web.events.ERROR
import web.events.Event
import web.events.EventHandler
import web.events.SEEKED
import web.events.addEventHandler
import web.html.Audio
import web.html.play
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

class WebPlayerItem(
    override val id: String,
    private val tempFile: PlatformMedia.TemporaryFile,
    private val coroutineScope: CoroutineScope,
    private val player: WebMediaPlayer,
    private val audio: Audio,
) : AbstractMediaItem(coroutineScope, player.playerMutex, player.currentItemPlaying) {
    override val duration: Duration = audio.duration.seconds
    private var updateJob: Job? = null

    private val removeErrorEventHandler: () -> Unit = audio.addEventHandler(
        type = Event.ERROR,
        handler = EventHandler {
            log.error { "Playback error: ${audio.error?.message}" }
            setError("Playback error")
        },
    )

    private val removeEndedEventHandler: () -> Unit = audio.addEventHandler(
        type = Event.ENDED,
        handler = EventHandler {
            log.debug { "Playback ended. Seeking to beginning..." }
            coroutineScope.launch {
                pause()
                seekTo(Duration.ZERO)
            }
        },
    )

    override suspend fun onPlay(duration: Duration): Result<Unit> {
        player.resumeAudioContext()

        onSeekTo(duration)
        return if (audio.error == null) {
            try {
                audio.play()
                updateJob = repeatedlyUpdateElapsedTime()
                Result.success(Unit)
            } catch (t: Throwable) {
                if (t is CancellationException) {
                    throw t
                }
                Result.failure(t)
            }
        } else {
            log.error { "Cannot play because of this previous error: ${audio.error?.message}" }
            Result.failure(IllegalStateException("Playback error"))
        }
    }

    override suspend fun onSeekTo(position: Duration) {
        log.debug { "Seeking media playback to position $position" }
        player.resumeAudioContext()

        if (isSeekableTo(audio, position)) {
            audio.currentTime = position.toDouble(DurationUnit.SECONDS)
            val timeout = withTimeoutOrNull(10.seconds) {
                suspendCancellableCoroutine { cont ->
                    handleFirst(
                        eventTarget = audio,
                        handlers = mapOf(
                            Event.ERROR to {
                                log.error { "Media is seekable but seeking failed: ${audio.error?.message}" }
                                cont.resume(Unit)
                            },
                            Event.SEEKED to {
                                cont.resume(Unit)
                            }
                        )
                    )
                }
            }
            if (timeout == null) {
                log.error { "Seeking timed out" }
            }
        } else {
            log.warn { "Media is not seekable to selected position. Not seeking" }
        }
    }

    override suspend fun onPause() {
        player.resumeAudioContext()

        audio.pause()
        updateJob?.cancel()
        updateJob = null
    }

    override suspend fun onClose() {
        updateJob?.cancel()
        updateJob = null
        removeErrorEventHandler()
        removeEndedEventHandler()
        tempFile.delete()
    }

    private fun isSeekableTo(audio: Audio, position: Duration): Boolean {
        val seekableRanges = audio.seekable
        val seekableRangesIndices = (0..<seekableRanges.length)
        val seekableRangesIterable = seekableRangesIndices.map { i ->
            Pair(seekableRanges.start(i), seekableRanges.end(i))
        }

        val positionSeconds = position.toDouble(DurationUnit.SECONDS)
        return seekableRangesIterable.map { (start, end) ->
            positionSeconds in start..end
        }
            .any { it }
    }

    private fun repeatedlyUpdateElapsedTime(): Job = coroutineScope.launch {
        while (isActive) {
            delay(150.milliseconds)
            elapsedTime.value = audio.currentTime.seconds
        }
    }
}
