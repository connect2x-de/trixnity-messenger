package de.connect2x.trixnity.messenger.media

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.lognity.api.logger.debug
import de.connect2x.lognity.api.logger.warn
import de.connect2x.trixnity.core.model.events.m.room.EncryptedFile
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import de.connect2x.trixnity.messenger.media.AudioRecorder.State.Completed.MediaReference
import de.connect2x.trixnity.utils.ByteArrayFlow
import io.ktor.http.*
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@TrixnityMessengerPrivateApi
interface AudioRecorder : AutoCloseable {
    val state: StateFlow<State>

    suspend fun startSuspending(intoMediaStore: suspend (ByteArrayFlow) -> MediaReference)

    suspend fun complete()

    suspend fun loadSuspending(state: State.Completed)

    suspend fun closeSuspending()

    sealed interface State {
        object Ready : State

        data class Recording(val duration: Duration, val loudness: Float) : State

        data class Completed(
            val media: MediaReference,
            val duration: Duration,
            val sizeBytes: Long?,
            val contentType: ContentType,
            val fileExtension: String,
        ) : State {
            sealed interface MediaReference {
                data class Unencrypted(val uri: String): MediaReference
                data class Encrypted(val uriWithMetadata: EncryptedFile): MediaReference
            }
        }
    }
}

class AudioRecorderHolder(val getOrNull: AudioRecorder?)

class AudioRecorderImpl(
    private val platformAudioRecorder: PlatformAudioRecorder,
    clock: Clock,
    private val parentScope: CoroutineScope,
) : AudioRecorder {
    private val stateImpl: MutableStateFlow<State> = MutableStateFlow(State.Ready)

    override val state: StateFlow<AudioRecorder.State> =
        stateImpl
            .sampleToPublicState(clock)
            .onEach { onMaxDuration(it) { complete() } }
            .stateIn(parentScope, SharingStarted.WhileSubscribed(), AudioRecorder.State.Ready)

    override suspend fun startSuspending(
        intoMediaStore: suspend (ByteArrayFlow) -> MediaReference
    ) {
        closeSuspending()

        val initialRecordingState = platformAudioRecorder.start(intoMediaStore)
        if (initialRecordingState != null) {
            stateImpl.value = withCatchCallbacks(initialRecordingState)
        }
    }

    override suspend fun loadSuspending(state: AudioRecorder.State.Completed) {
        closeSuspending()

        platformAudioRecorder.load(state)?.let { stateImpl.value = it }
    }

    override suspend fun complete() {
        stateImpl.value = complete(stateImpl.value)
    }

    override suspend fun closeSuspending() {
        withContext(NonCancellable) {
            stateImpl.value = close(stateImpl.value)
            platformAudioRecorder.close()
        }
    }

    override fun close() {
        parentScope.launch {
            closeSuspending()
        }
    }

    /** Abstract effectful platform-specific actions by storing them here as function values */
    sealed interface State {
        object Ready : State

        data class Recording(val start: Instant, val loudness: () -> Float?, val complete: suspend (Recording) -> Completed?) :
            State

        data class Completed(
            val capture: MediaReference,
            val duration: Duration,
            val sizeBytes: Long?,
            val contentType: ContentType,
            val fileExtension: String,
            val deleteCapture: () -> Unit,
        ) : State
    }

    data class Format<Container, Encoder>(
        val container: Container,
        val encoder: Encoder,
        val sampleRate: SampleRateHz,
        val bitRate: BitRate,
        val contentType: ContentType,
    ) {
        enum class SampleRateHz(val value: Int) {
            AAC_SAMPLING_RATE_HZ(44_100)
        }

        enum class BitRate(val value: Int) {
            AAC_BIT_RATE(32_000)
        }
    }

    companion object {
        private val log: Logger = Logger("de.connect2x.trixnity.messenger.media.AudioRecorder")

        private suspend fun complete(stateImpl: State): State {
            return when (stateImpl) {
                State.Ready -> {
                    log.debug { "Audio recorder not started" }
                    State.Ready
                }

                is State.Recording -> {
                    log.debug { "Stopping audio recorder" }

                    val completedState =
                        try {
                            stateImpl.complete(stateImpl)
                        } catch (t: Throwable) {
                            log.warn(t) { "Completing audio recording failed." }
                            null
                        }
                    if (completedState != null) {
                        completedState
                    } else {
                        log.warn { "Stopping audio recorder failed" }
                        State.Ready
                    }
                }

                is State.Completed -> {
                    log.debug { "Audio recorder already stopped" }
                    stateImpl
                }
            }
        }

        private suspend fun close(stateImpl: State): State {
            log.debug { "Cleaning audio recorder" }
            when (val completed = complete(stateImpl)) {
                is State.Completed ->
                    try {
                        completed.deleteCapture()
                    } catch (t: Throwable) {
                        log.warn(t) { "Failed to close audio recorder" }
                    }

                State.Ready -> Unit
                is State.Recording -> Unit
            }

            return State.Ready
        }

        /** Sampling so that the public API ([AudioRecorder.State]) can be immutable */
        private fun Flow<State>.sampleToPublicState(clock: Clock): Flow<AudioRecorder.State> {
            @OptIn(ExperimentalCoroutinesApi::class)
            fun emitRepeatedlyWhileRecording(stateImpl: Flow<State>): Flow<State> {
                return stateImpl.transformLatest { state ->
                    when (state) {
                        is State.Recording ->
                            while (currentCoroutineContext().isActive) {
                                emit(state)
                                delay(50.milliseconds)
                            }

                        is State.Completed,
                        State.Ready -> {
                            emit(state)
                        }
                    }
                }
            }

            fun toPublicState(stateImpl: State): AudioRecorder.State {
                return when (stateImpl) {
                    is State.Recording -> {
                        AudioRecorder.State.Recording(
                            duration = clock.now() - stateImpl.start,
                            loudness = stateImpl.loudness() ?: 0f,
                        )
                    }

                    State.Ready -> AudioRecorder.State.Ready
                    is State.Completed ->
                        AudioRecorder.State.Completed(
                            stateImpl.capture,
                            stateImpl.duration,
                            stateImpl.sizeBytes,
                            stateImpl.contentType,
                            stateImpl.fileExtension,
                        )
                }
            }

            return emitRepeatedlyWhileRecording(this).map { toPublicState(it) }
        }

        private suspend fun onMaxDuration(state: AudioRecorder.State, callback: suspend () -> Unit) {
            when (state) {
                is AudioRecorder.State.Recording -> {
                    if (state.duration >= 5.hours) {
                        callback()
                    }
                }

                is AudioRecorder.State.Completed,
                AudioRecorder.State.Ready -> Unit
            }
        }

        private fun withCatchCallbacks(recordingState: State.Recording): State.Recording {
            return recordingState.copy(
                loudness = {
                    try {
                        recordingState.loudness()
                    } catch (e: Throwable) {
                        log.debug(e) { "Getting audio loudness failed" }
                        null
                    }
                },
                complete = {
                    try {
                        recordingState.complete(it)
                    } catch (e: Throwable) {
                        log.warn(e) { "Completing audio recording failed." }
                        null
                    }
                },
            )
        }
    }
}
