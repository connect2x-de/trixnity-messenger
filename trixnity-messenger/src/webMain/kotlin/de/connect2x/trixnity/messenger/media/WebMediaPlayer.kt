package de.connect2x.trixnity.messenger.media

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.lognity.api.logger.error
import de.connect2x.trixnity.client.media.PlatformMedia
import de.connect2x.trixnity.client.media.indexeddb.IndexeddbPlatformMedia
import de.connect2x.trixnity.client.media.opfs.OpfsPlatformMedia
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import web.audio.AudioContext
import web.audio.AudioContextState
import web.audio.resume
import web.audio.suspended
import web.events.EventHandler
import web.html.Audio
import web.html.Preload
import web.html.metadata
import web.url.URL
import kotlin.coroutines.resume
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class WebMediaPlayer(parentScope: CoroutineScope) : MediaPlayer {
    private val log: Logger = Logger("de.connect2x.trixnity.messenger.media.WebMediaPlayer")
    internal val currentItemPlaying: MutableStateFlow<AbstractMediaItem?> = MutableStateFlow(null)
    internal val playerMutex: Mutex = Mutex()
    private val coroutineScope: CoroutineScope = CoroutineScope(
            parentScope.coroutineContext + SupervisorJob(parentScope.coroutineContext[Job])
        )

    private val audioContext: AudioContext = AudioContext()

    override val playingItem: StateFlow<MediaPlayer.Item?> = currentItemPlaying.asStateFlow()

    override suspend fun open(
        id: String,
        media: PlatformMedia,
        mimeType: String,
        lifecycleScope: CoroutineScope?
    ): Result<MediaPlayer.Item> {
        val playingItem = playingItem.value
        if (playingItem != null && playingItem.id == id) {
            playingItem.updateLifecycle(lifecycleScope)
            return Result.success(playingItem)
        }

        val scopeWithExceptionHandler = run {
            val coroutineContext = coroutineScope.coroutineContext
            val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
                log.error(throwable) { "Unexpected error while running media player" }
            }
            CoroutineScope(coroutineContext + Job(coroutineContext[Job]) + exceptionHandler)
        }

        media.getTemporaryFile().fold(
            onFailure = {
                log.error(it) { "Unable to open media as temporary file" }
                return Result.failure(it)
            },
            onSuccess = { tempFile ->
                log.debug { "Successfully opened media as temporary file" }
                try {
                    val audio = initAudio(tempFile)
                    val duration = loadDuration(audio)
                    val playerItem = WebPlayerItem(
                        id = id,
                        tempFile = tempFile,
                        coroutineScope = scopeWithExceptionHandler,
                        player = this@WebMediaPlayer,
                        audio = audio,
                        duration = duration,
                    )
                    playerItem.updateLifecycle(lifecycleScope)
                    return Result.success(playerItem)
                } catch (ex: Exception) {
                    return Result.failure(IllegalArgumentException("Illegal media specified", ex))
                }
            }
        )
    }

    /**
     * Context could have been suspended because of autoplay policy. See: https://developer.mozilla.org/en-US/docs/Web/API/Web_Audio_API/Using_Web_Audio_API#controlling_sound
     */
    @OptIn(ExperimentalWasmJsInterop::class)
    suspend fun resumeAudioContext() {
        if (audioContext.state == AudioContextState.suspended) {
            audioContext.resume()
        }
    }

    override fun close() {
        audioContext.closeAsync()
    }

    private fun initAudio(tempFile: PlatformMedia.TemporaryFile): Audio {
        val audioBlob = when (tempFile) {
            is OpfsPlatformMedia.TemporaryFile -> tempFile.file
            is IndexeddbPlatformMedia.TemporaryFile -> tempFile.file
            else -> error("PlatformMedia is required to be a OpfsPlatformMedia or IndexeddbPlatformMedia")
        }
        val audio = Audio(URL.createObjectURL(audioBlob))
        audio.preload = Preload.metadata
        val track = audioContext.createMediaElementSource(audio)
        track.connect(audioContext.destination)
        return audio
    }

    private suspend fun loadDuration(audio: Audio): Duration {
        return suspendCancellableCoroutine { cont ->
            audio.onloadedmetadata = EventHandler { _ ->
                cont.resume(audio.duration.seconds)
            }
            cont.invokeOnCancellation { audio.onloadedmetadata = null }
        }
    }
}
