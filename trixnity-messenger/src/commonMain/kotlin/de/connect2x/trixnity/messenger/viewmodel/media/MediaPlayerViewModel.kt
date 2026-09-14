package de.connect2x.trixnity.messenger.viewmodel.media

import de.connect2x.lognity.api.logger.error
import de.connect2x.trixnity.client.media.PlatformMedia
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.media.MediaPlayer
import de.connect2x.trixnity.messenger.util.getOrNull
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import kotlin.time.Duration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.component.get

interface MediaPlayerViewModelFactory {
    fun create(
        id: String,
        viewModelContext: MatrixClientViewModelContext,
        mimeType: String,
        initialDuration: Duration?,
        acquireFile: suspend () -> Result<PlatformMedia>,
    ): MediaPlayerViewModel? {
        val config = viewModelContext.get<MatrixMessengerConfiguration>()
        return if (config.features.enableMediaPlayer) {
            MediaPlayerViewModelImpl(
                id = id,
                viewModelContext = viewModelContext,
                mimeType = mimeType,
                initialDurationOptional = initialDuration,
                acquireFile = acquireFile,
            )
        } else {
            null
        }
    }

    companion object : MediaPlayerViewModelFactory
}

interface MediaPlayerViewModel : AutoCloseable {
    val elapsedTime: StateFlow<Duration>
    val duration: StateFlow<Duration>
    val state: StateFlow<State>

    fun play()

    fun pause()

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
    private val id: String,
    private val mimeType: String,
    initialDurationOptional: Duration?,
    private val acquireFile: suspend () -> Result<PlatformMedia>,
) : MediaPlayerViewModel, MatrixClientViewModelContext by viewModelContext {
    private val player: MediaPlayer? = getOrNull()
    private val item: MutableStateFlow<MediaPlayer.Item?> = MutableStateFlow(null)
    private val mutex: Mutex = Mutex()

    @OptIn(ExperimentalCoroutinesApi::class)
    override val elapsedTime: StateFlow<Duration> =
        item
            .flatMapLatest { it?.elapsedTime ?: flowOf(Duration.ZERO) }
            .map { it ?: Duration.ZERO }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), Duration.ZERO)

    override val duration: MutableStateFlow<Duration> = MutableStateFlow(initialDurationOptional ?: Duration.ZERO)

    override val state: MutableStateFlow<MediaPlayerViewModel.State> =
        MutableStateFlow(if (player != null) MediaPlayerViewModel.State.Ready else MediaPlayerViewModel.State.NotReady)

    init {
        coroutineScope.launch {
            mutex.withLock {
                if (player?.playingItem?.value?.id == id) {
                    acquireMedia().fold(onFailure = ::handleAcquireFailure, onSuccess = { item.value = it })
                }
            }
        }
    }

    override fun play() {
        coroutineScope.launch { mutex.withLock { acquireMediaItemIfAbsent()?.play() } }
    }

    override fun pause() {
        if (item.value == null) {
            log.error { "Unable to pause playback of media file because the media item is not present" }
            return
        }

        coroutineScope.launch { mutex.withLock { item.value?.pause() } }
    }

    override fun seekTo(position: Duration) {
        coroutineScope.launch { mutex.withLock { acquireMediaItemIfAbsent()?.seekTo(position) } }
    }

    override fun close() {
        coroutineScope.launch { mutex.withLock { item.value?.close() } }
    }

    private suspend fun acquireMediaItemIfAbsent(): MediaPlayer.Item? {
        item.value?.let {
            return it
        }

        log.debug { "Media item is not present, downloading item" }
        return acquireMedia()
            .fold(
                onFailure = {
                    handleAcquireFailure(it)
                    null
                },
                onSuccess = {
                    log.debug { "Successfully downloaded media" }
                    item.value = it
                    it
                },
            )
    }

    private suspend fun acquireMedia(): Result<MediaPlayer.Item?> =
        acquireFile().mapCatching { media ->
            player?.open(id, media, mimeType, coroutineScope)?.getOrThrow()?.also(::listenForItemState)
        }

    private fun handleAcquireFailure(cause: Throwable) {
        log.error(cause) { "Unable to download media" }
        state.value = MediaPlayerViewModel.State.Failure(cause.message ?: "Unable to download media")
    }

    private fun listenForItemState(item: MediaPlayer.Item) {
        duration.value = item.duration
        coroutineScope.launch {
            item.state.collect { itemState ->
                state.value =
                    when (itemState) {
                        is MediaPlayer.Item.State.Ready -> MediaPlayerViewModel.State.Ready
                        is MediaPlayer.Item.State.Playing -> MediaPlayerViewModel.State.Playing
                        is MediaPlayer.Item.State.Failed -> MediaPlayerViewModel.State.Failure(itemState.message)
                    }
            }
        }
    }
}
