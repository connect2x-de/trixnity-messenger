package de.connect2x.trixnity.messenger.viewmodel.media

import com.arkivanov.essenty.lifecycle.doOnDestroy
import de.connect2x.trixnity.messenger.media.MediaPlayer
import de.connect2x.trixnity.messenger.util.getOrNull
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.media.AudioPlayerViewModel.State
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.util.GetAmplitudes
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.folivo.trixnity.client.media.PlatformMedia
import okio.Path.Companion.toPath
import org.koin.core.component.get

import kotlin.time.Duration

private val log = KotlinLogging.logger { }

interface AudioPlayerViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        audio: RoomMessageTimelineElementViewModel.FileBased.Audio,
        initialDuration: Duration?
    ) : AudioPlayerViewModel = AudioPlayerViewModelImpl(
        viewModelContext = viewModelContext,
        audio = audio,
        initialDuration = initialDuration
    )

    companion object : AudioPlayerViewModelFactory
}

interface AudioPlayerViewModel {
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
        fun wasLoaded(): Boolean = this is Ready || this is Playing

        object Loading : State
        object NotAvailable : State

        data class Failed(val cause: Throwable? = null) : State

        /**
         * @param amplitudes the normalized amplitudes (0.0 to 1.0) of the audio file
         */
        data class Ready(val amplitudes: List<Float>) : State

        /**
         * @param amplitudes the normalized amplitudes (0.0 to 1.0) of the audio file
         */
        data class Playing(val amplitudes: List<Float>) : State
    }
}

class AudioPlayerViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val audio: RoomMessageTimelineElementViewModel.FileBased.Audio,
    initialDuration: Duration?
) : AudioPlayerViewModel, MatrixClientViewModelContext by viewModelContext {
    private val player: MediaPlayer? = getOrNull()
    private val stateChangeMutex: Mutex = Mutex()
    private var updateJob: Job? = null

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
                audio.downloadMedia { media ->
                    val getAmplitudes: GetAmplitudes = get()

                    platformMedia.value = media
                    val amplitudes = getAmplitudes(media, 200).getOrThrow()
                    state.value = State.Ready(amplitudes)
                }
            }
        }
    }

    override fun start() {
        checkNotNull(player) { "The player should not be null when starting playing" }
        coroutineScope.launch {
            stateChangeMutex.withLock {
                check(state.value is State.Ready) { "The player is not ready or already playing" }

                log.info { "Start playing media '${audio.name}' with media player" }
                player.start(requireNotNull(platformMedia.value), audio.mimeType, elapsedTime.value)
                state.value = State.Playing((state.value as State.Ready).amplitudes)

                updateJob?.cancel()
                updateJob = coroutineScope.launch(CoroutineName("Media Player Update Job")) {
                    launch {
                        player.elapsedTime.collect {
                            if (state.value !is State.Playing)
                                return@collect
                            elapsedTime.value = it
                        }
                    }
                    launch {
                        player.duration.collect {
                            if (state.value !is State.Playing)
                                return@collect
                            duration.value = it
                        }
                    }
                }
            }
        }
    }

    override fun stop() {
        checkNotNull(player) { "The player should not be null when stopping playing" }
        coroutineScope.launch {
            stateChangeMutex.withLock {
                check(state.value is State.Playing) { "The player is not ready or not playing" }

                log.info { "Stop playing media '${audio.name}' with media player" }
                state.value = State.Ready((state.value as State.Playing).amplitudes)
                player.stop()
            }
        }
    }

    override fun seekTo(duration: Duration) {
        checkNotNull(player) { "The player should not be null when stopping playing" }
        check(state.value.wasLoaded()) { "The player is not ready" }
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
