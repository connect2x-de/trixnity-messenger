package de.connect2x.trixnity.messenger.viewmodel.media

import com.arkivanov.essenty.lifecycle.Lifecycle
import de.connect2x.trixnity.messenger.media.MediaPlayer
import de.connect2x.trixnity.messenger.util.getOrNull
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
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
        class Failure(val cause: String) : State()
    }
}

class MediaPlayerViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val media: RoomMessageTimelineElementViewModel.FileBased<*>,
    initialDurationOptional: Duration?
) : MediaPlayerViewModel, MatrixClientViewModelContext by viewModelContext {
    private val globalScope: CoroutineScope = get()
    private val isAudio = media is RoomMessageTimelineElementViewModel.FileBased.Audio
    private val mimeType = media.mimeType ?: if (isAudio) "audio/raw" else "video/raw"

    private val player: MediaPlayer? = getOrNull()
    private val item: MutableStateFlow<MediaPlayer.Item?> = MutableStateFlow(null)
    private val initialDuration = initialDurationOptional ?: Duration.ZERO
    private val mutex: Mutex = Mutex()

    @OptIn(ExperimentalCoroutinesApi::class)
    override val elapsedTime: StateFlow<Duration> = item
        .flatMapLatest { it?.elapsedTime ?: flowOf(Duration.ZERO) }
        .map { it ?: Duration.ZERO }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), Duration.ZERO)

    @OptIn(ExperimentalCoroutinesApi::class)
    override val duration: StateFlow<Duration> = item
        .flatMapLatest { it?.duration ?: flowOf(initialDuration) }
        .map { it ?: initialDuration }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), initialDuration)

    override val state: MutableStateFlow<MediaPlayerViewModel.State> =
        MutableStateFlow(if (player != null) MediaPlayerViewModel.State.Ready else MediaPlayerViewModel.State.NotReady)

    init {
        lifecycle.subscribe(object : Lifecycle.Callbacks {
            override fun onDestroy() {
                globalScope.launch {
                    item.value?.close()
                }
            }
        })

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
        coroutineScope.launch {
            mutex.withLock {
                if (state.value !is MediaPlayerViewModel.State.Ready) {
                    log.error { "Unable to play media because its already playing" }
                    return@withLock
                }

                if (item.value == null) {
                    log.debug { "Media item is not present, downloading item" }
                    openMedia().fold(
                        onFailure = {
                            log.error(it) { "Unable to download media" }
                            val message = it.message ?: "Unable to download media"
                            state.value = MediaPlayerViewModel.State.Failure(message)
                            return@launch
                        },
                        onSuccess = {
                            log.debug { "Successfully downloaded media" }
                            item.value = it
                        }
                    )
                }

                item.value?.play()
                state.value = MediaPlayerViewModel.State.Playing
            }
        }
    }

    override fun stop() {
        if (item.value == null) {
            log.error { "Unable to start playback of media file because the media player is not present" }
            return
        }

        coroutineScope.launch {
            mutex.withLock {
                if (state.value !is MediaPlayerViewModel.State.Playing) {
                    log.error { "Unable to stop media because its not playing" }
                    return@withLock
                }

                item.value?.pause()
                state.value = MediaPlayerViewModel.State.Ready
            }
        }
    }

    override fun seekTo(position: Duration) {
        coroutineScope.launch {
            mutex.withLock {
                item.value?.seekTo(position)
            }
        }
    }

    private suspend fun openMedia(): Result<MediaPlayer.Item?> = suspendCancellableCoroutine { continuation ->
        media.downloadMedia(
            onDownloadCancelled = {
                continuation.resume(Result.failure(Exception("Download was cancelled"))) { _, _, _ -> }
            },
            processFile = { media ->
                val item = player?.open(media, mimeType, coroutineScope) ?: Result.success(null)
                continuation.resume(item) { _, _, _ -> }
            }
        )
    }
}
