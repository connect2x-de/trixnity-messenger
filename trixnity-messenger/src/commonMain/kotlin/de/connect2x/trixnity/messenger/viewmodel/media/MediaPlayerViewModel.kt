package de.connect2x.trixnity.messenger.viewmodel.media

import com.arkivanov.essenty.lifecycle.Lifecycle
import de.connect2x.trixnity.messenger.media.MediaPlayer
import de.connect2x.trixnity.messenger.util.getOrNull
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.component.get
import kotlin.time.Duration

private val log = KotlinLogging.logger { }

interface MediaPlayerViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        media: RoomMessageTimelineElementViewModel.FileBased<*>,
        initialDuration: Duration?
    ) : MediaPlayerViewModel = MediaPlayerViewModelImpl(
        viewModelContext = viewModelContext,
        media = media,
        initialDurationOptional = initialDuration
    )

    companion object : MediaPlayerViewModelFactory
}

interface MediaPlayerViewModel {
    val elapsedTime: StateFlow<Duration>
    val duration: StateFlow<Duration>
    val state: StateFlow<State>

    fun start()
    fun stop()
    fun seekTo(position: Duration)

    sealed class State {
        object NotReady : State()
        object Playing : State()
        object Ready : State()
    }
}

class MediaPlayerViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    media: RoomMessageTimelineElementViewModel.FileBased<*>,
    initialDurationOptional: Duration?
) : MediaPlayerViewModel, MatrixClientViewModelContext by viewModelContext {
    private val globalScope: CoroutineScope = get()

    private val player: MediaPlayer? = getOrNull()
    private val item: MediaPlayer.Item? = player?.open(media)
    private val initialDuration = initialDurationOptional ?: Duration.ZERO
    private val mutex: Mutex = Mutex()

    override val elapsedTime: StateFlow<Duration> =
        item?.elapsedTime?.map { it ?: Duration.ZERO }
            ?.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), Duration.ZERO)
            ?: MutableStateFlow(Duration.ZERO)
    override val duration: StateFlow<Duration> =
        item?.duration?.map { it ?: initialDuration }
            ?.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), initialDuration)
            ?: MutableStateFlow(initialDuration)
    override val state: MutableStateFlow<MediaPlayerViewModel.State> =
        MutableStateFlow(if (player != null) MediaPlayerViewModel.State.Ready else MediaPlayerViewModel.State.NotReady)

    init {
        lifecycle.subscribe(object : Lifecycle.Callbacks {
            override fun onDestroy() {
                globalScope.launch {
                    item?.close()
                }
            }
        })

        if (player == null || item == null) {
            state.value = MediaPlayerViewModel.State.NotReady // Not available
        }

        player?.playingItem?.let {
            coroutineScope.launch {
                it.collect { playingItem ->
                    if (playingItem == null && state.value is MediaPlayerViewModel.State.Playing) {
                        state.value = MediaPlayerViewModel.State.Ready
                    }
                }
            }
        }
    }

    override fun start() {
        if (item == null) {
            log.error { "Unable to start playback of media file because the media player is not present" }
            return
        }

        coroutineScope.launch {
            mutex.withLock {
                if (state.value !is MediaPlayerViewModel.State.Ready) {
                    log.error { "Unable to play media because its already playing" }
                    return@withLock
                }

                item.play()
                state.value = MediaPlayerViewModel.State.Playing
            }
        }
    }

    override fun stop() {
        if (item == null) {
            log.error { "Unable to start playback of media file because the media player is not present" }
            return
        }

        coroutineScope.launch {
            mutex.withLock {
                if (state.value !is MediaPlayerViewModel.State.Playing) {
                    log.error { "Unable to stop media because its not playing" }
                    return@withLock
                }

                item.pause()
                state.value = MediaPlayerViewModel.State.Ready
            }
        }
    }

    override fun seekTo(position: Duration) {
        if (item == null) {
            log.error { "Unable to seek media file because the media player is not present" }
            return
        }

        coroutineScope.launch {
            mutex.withLock {
                item.seekTo(position)
            }
        }
    }
}
