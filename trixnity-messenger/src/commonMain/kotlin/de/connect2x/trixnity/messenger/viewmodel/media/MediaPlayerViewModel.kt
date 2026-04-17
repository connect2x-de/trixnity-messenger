package de.connect2x.trixnity.messenger.viewmodel.media

import de.connect2x.lognity.api.logger.error
import de.connect2x.trixnity.client.media.PlatformMedia
import de.connect2x.trixnity.messenger.media.MediaPlayer
import de.connect2x.trixnity.messenger.util.getOrNull
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
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
import kotlin.time.Duration

interface MediaPlayerViewModelFactory {
    fun create(
        id: String,
        viewModelContext: MatrixClientViewModelContext,
        mimeType: String,
        initialDuration: Duration?,
        acquireFile: suspend () -> Result<PlatformMedia>
    ) : MediaPlayerViewModel = MediaPlayerViewModelImpl(
        id = id,
        viewModelContext = viewModelContext,
        mimeType = mimeType,
        initialDurationOptional = initialDuration,
        acquireFile = acquireFile
    )

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
    private val acquireFile: suspend () -> Result<PlatformMedia>
) : MediaPlayerViewModel, MatrixClientViewModelContext by viewModelContext {
    private val player: MediaPlayer? = getOrNull()
    private val item: MutableStateFlow<MediaPlayer.Item?> = MutableStateFlow(null)
    private val mutex: Mutex = Mutex()

    @OptIn(ExperimentalCoroutinesApi::class)
    override val elapsedTime: StateFlow<Duration> = item
        .flatMapLatest { it?.elapsedTime ?: flowOf(Duration.ZERO) }
        .map { it ?: Duration.ZERO }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), Duration.ZERO)

    override val duration: MutableStateFlow<Duration> = MutableStateFlow(initialDurationOptional ?: Duration.ZERO)

    override val state: MutableStateFlow<MediaPlayerViewModel.State> =
        MutableStateFlow(if (player != null) MediaPlayerViewModel.State.Ready else MediaPlayerViewModel.State.NotReady)

    init {
        coroutineScope.launch {
            mutex.withLock {
                player?.playingItem?.let { item ->
                    val itemValue = item.value

                    // In this case, we acquire the media only when the currently played media's id is equal to the id
                    // of the viewmodel. So this only calls open, which in this case returns the media being currently
                    // played.
                    if (itemValue != null && id == itemValue.id) {
                        acquireMedia().fold(
                            onFailure = {
                                log.error(it) { "Unable to download media" }
                                val message = it.message ?: "Unable to download media"
                                state.value = MediaPlayerViewModel.State.Failure(message)
                            },
                            onSuccess = {
                                this@MediaPlayerViewModelImpl.item.value = it
                            }
                        )
                    }
                }
            }
        }
    }

    override fun play() {
        coroutineScope.launch {
            mutex.withLock {
                if (item.value == null) {
                    log.debug { "Media item is not present, downloading item" }
                    acquireMedia().fold(
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

    override fun pause() {
        if (item.value == null) {
            log.error { "Unable to start playback of media file because the media player is not present" }
            return
        }

        coroutineScope.launch {
            mutex.withLock {
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

    override fun close() {
        coroutineScope.launch {
            mutex.withLock {
                item.value?.close()
            }
        }
    }

    private suspend fun acquireMedia(): Result<MediaPlayer.Item?> = acquireFile().fold(
        onFailure = { Result.failure(it) },
        onSuccess = {
            val item = player?.open(id, it, mimeType, coroutineScope) ?: Result.success(null)
            if (item.isSuccess) {
                item.getOrNull()?.let { item ->
                    listenForItemState(item)
                }
            }

            return item
        }
    )

    private fun listenForItemState(item: MediaPlayer.Item) {
        duration.value = item.duration
        coroutineScope.launch {
            item.state.collect { itemState ->
                state.value = when (itemState) {
                    is MediaPlayer.Item.State.Ready -> MediaPlayerViewModel.State.Ready
                    is MediaPlayer.Item.State.Playing -> MediaPlayerViewModel.State.Playing
                    is MediaPlayer.Item.State.Failed -> MediaPlayerViewModel.State.Failure(itemState.message)
                }
            }
        }
    }
}
