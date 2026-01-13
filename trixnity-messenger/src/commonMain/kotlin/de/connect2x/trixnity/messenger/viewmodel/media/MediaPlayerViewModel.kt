package de.connect2x.trixnity.messenger.viewmodel.media

import com.arkivanov.essenty.lifecycle.doOnDestroy
import de.connect2x.trixnity.messenger.media.MediaPlayer
import de.connect2x.trixnity.messenger.util.getOrNull
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.media.MediaPlayerViewModel.State
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.folivo.trixnity.client.media.PlatformMedia
import org.koin.core.component.get

import kotlin.time.Duration

private val log = KotlinLogging.logger { }

interface MediaPlayerViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        audio: RoomMessageTimelineElementViewModel.FileBased.Audio,
        initialDuration: Duration?
    ) : MediaPlayerViewModel = MediaPlayerViewModelImpl(
        viewModelContext = viewModelContext,
        audio = audio,
        initialDuration = initialDuration
    )

    companion object : MediaPlayerViewModelFactory
}

interface MediaPlayerViewModel {
    /**
     * This field represents the elapsed time while playing the media. When paused, the elapsed time duration
     * should be preserved to the time played allowing to start at that time when re-starting audio playback.
     */
    val elapsedTime: StateFlow<Duration>
    val duration: StateFlow<Duration>
    val state: StateFlow<State>

    fun start()
    fun stop()

    fun seekTo(duration: Duration)

    /**
     * Loading -> Failed / Ready -> Playing <-> Ready
     */
    sealed interface State {
        object Loading : State
        object NotAvailable : State

        data class Failed(val cause: String? = null) : State
        data object Ready : State
        data object Playing : State
    }
}

class MediaPlayerViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val audio: RoomMessageTimelineElementViewModel.FileBased.Audio,
    initialDuration: Duration?
) : MediaPlayerViewModel, MatrixClientViewModelContext by viewModelContext {
    private val player: MediaPlayer? = getOrNull()
    private val stateChangeMutex: Mutex = Mutex()

    private val platformMedia: MutableStateFlow<PlatformMedia?> = MutableStateFlow(null)
    override val elapsedTime: MutableStateFlow<Duration> = MutableStateFlow(Duration.ZERO)
    override val duration: MutableStateFlow<Duration> = MutableStateFlow(initialDuration ?: Duration.ZERO)
    override val state: MutableStateFlow<State> = MutableStateFlow(player?.let { State.Loading } ?: State.NotAvailable)

    init {
        lifecycle.doOnDestroy {
            if (player != null && state.value is State.Playing) {
                get<CoroutineScope>().launch {
                    log.debug { "Audio player still playing, stop playing and dispose resources" }
                    stop()
                }
            }
        }

        if (player != null) {
            log.debug { "Initiating download of media '${audio.name}' for media player" }
            coroutineScope.launch {
                audio.downloadMedia(
                    processFile = { media ->
                        platformMedia.value = media
                        state.value = State.Ready
                    },
                    onDownloadCancelled = {
                        state.value = State.Failed()
                    }
                )
            }
        }
    }

    override fun start() {
        if (player == null) {
            log.error { "Unable to start playback of media file because the media player is not present" }
            return
        }

        coroutineScope.launch {
            stateChangeMutex.withLock {
                check(state.value is State.Ready) { "The player is not ready or already playing" }

                log.info { "Start playing media '${audio.name}' with media player" }
                val startTime = elapsedTime.value
                player.start(requireNotNull(platformMedia.value), audio.mimeType, elapsedTime.value) { event ->
                    when (event) {
                        is MediaPlayer.Event.Stopped -> stop()
                        is MediaPlayer.Event.Progress -> {
                            println("EEEEEEEEE: ${event.elapsedTime} of ${event.duration} (ST = $startTime)")
                            elapsedTime.value = event.elapsedTime
                            if (event.duration > Duration.ZERO) {
                                duration.value = event.duration
                            }
                        }
                    }
                }
                state.value = State.Playing
            }
        }
    }

    override fun stop() {
        if (player == null) {
            log.error { "Unable to start playback of media file because the media player is not present" }
            return
        }

        coroutineScope.launch {
            stateChangeMutex.withLock {
                if (state.value !is State.Playing) {
                    log.warn { "Unable to stop audio player because nothing is being played" }
                    return@withLock
                }

                log.info { "Stop playing media '${audio.name}' with media player" }
                state.value = State.Ready
                player.stop()
            }
        }
    }

    override fun seekTo(duration: Duration) {
        if (player == null) {
            log.error { "Unable to seek media file because the media player is not present" }
            return
        }

        coroutineScope.launch {
            log.debug { "Seeking media player to position '$duration' for media '${audio.name}'" }
            when (state.value) {
                is State.Playing -> player.seekTo(duration)
                is State.Ready -> elapsedTime.value = duration
                else -> {}
            }
        }
    }
}
