package de.connect2x.trixnity.messenger.media

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.lognity.api.logger.error
import de.connect2x.trixnity.client.media.PlatformMedia
import de.connect2x.trixnity.client.media.indexeddb.IndexeddbPlatformMedia
import de.connect2x.trixnity.client.media.opfs.OpfsPlatformMedia
import de.connect2x.trixnity.messenger.util.handleFirst
import js.errors.toThrowable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeout
import web.audio.AudioContext
import web.audio.AudioContextState
import web.audio.resume
import web.audio.suspended
import web.events.ERROR
import web.events.Event
import web.events.EventHandler
import web.events.LOADED_METADATA
import web.events.addEventHandler
import web.html.Audio
import web.html.Preload
import web.html.metadata
import web.url.URL
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.time.Duration.Companion.minutes

class WebMediaPlayer(private val coroutineScope: CoroutineScope) : MediaPlayer {
    private val log: Logger = Logger("de.connect2x.trixnity.messenger.media.WebMediaPlayer")
    internal val currentItemPlaying: MutableStateFlow<AbstractMediaItem?> = MutableStateFlow(null)
    internal val playerMutex: Mutex = Mutex()

    private val audioContext: AudioContext = AudioContext()

    override val playingItem: StateFlow<MediaPlayer.Item?> = currentItemPlaying.asStateFlow()

    init {
        audioContext.addEventHandler(
            type = Event.ERROR,
            handler = EventHandler {
                log.error { "Unexpected media player error" }
            },
        )
    }

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

        media.getTemporaryFile().fold(
            onFailure = {
                log.error(it) { "Unable to open media as temporary file" }
                return Result.failure(it)
            },
            onSuccess = { tempFile ->
                log.debug { "Successfully opened media as temporary file" }
                try {
                    val audio = initAudio(tempFile)
                    val playerItem = WebPlayerItem(
                        id = id,
                        tempFile = tempFile,
                        coroutineScope = coroutineScope,
                        player = this@WebMediaPlayer,
                        audio = audio,
                    )
                    playerItem.updateLifecycle(lifecycleScope)
                    log.debug { "Successfully initialized audio" }
                    return Result.success(playerItem)
                } catch (t: Throwable) {
                    if (t is CancellationException) {
                        throw t
                    }
                    return Result.failure(IllegalArgumentException("Illegal media specified", t))
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

    private suspend fun initAudio(tempFile: PlatformMedia.TemporaryFile): Audio {
        val audioBlob = when (tempFile) {
            is OpfsPlatformMedia.TemporaryFile -> tempFile.file
            is IndexeddbPlatformMedia.TemporaryFile -> tempFile.file
            else -> error("PlatformMedia is required to be a OpfsPlatformMedia or IndexeddbPlatformMedia")
        }
        val audio = Audio(URL.createObjectURL(audioBlob))
        audio.preload = Preload.metadata
        val track = audioContext.createMediaElementSource(audio)
        track.connect(audioContext.destination)
        loadMetadataOrElseThrow(audio)
        return audio
    }

    /**
     * Ensures that duration is set and media is supported
     */
    private suspend fun loadMetadataOrElseThrow(audio: Audio) {
        return withTimeout(2.minutes) {
            suspendCancellableCoroutine { cont ->
                handleFirst(
                    eventTarget = audio,
                    handlers = mapOf(
                        Event.ERROR to {
                            log.error { "Could not load duration: ${audio.error?.message}" }
                            cont.resumeWithException(audio.error?.toThrowable() ?: IllegalStateException("Could not load duration"))
                        },
                        Event.LOADED_METADATA to {
                            cont.resume(Unit)
                        }
                    )
                )

            }
        }
    }
}
