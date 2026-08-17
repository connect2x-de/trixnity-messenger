package de.connect2x.trixnity.messenger.media

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.lognity.api.logger.debug
import de.connect2x.lognity.api.logger.error
import de.connect2x.lognity.api.logger.warn
import de.connect2x.trixnity.core.model.events.m.room.EncryptedFile
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.media.AudioRecorder.State.Completed.MediaReference
import de.connect2x.trixnity.messenger.media.AudioRecorderImpl.Companion.toPublicState
import de.connect2x.trixnity.utils.ByteArrayFlow
import io.ktor.http.*
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
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

    suspend fun start(intoMediaStore: suspend (ByteArrayFlow) -> MediaReference)

    suspend fun complete()

    suspend fun load(state: State.Completed)

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
                /**
                 * @param uri Should be produced by [de.connect2x.trixnity.client.media.MediaService.prepareUploadMedia]
                 */
                data class Unencrypted(val uri: String) : MediaReference

                /**
                 * @param uriWithMetadata Should be produced by
                 *   [de.connect2x.trixnity.client.media.MediaService.prepareUploadEncryptedMedia]
                 */
                data class Encrypted(val uriWithMetadata: EncryptedFile) : MediaReference
            }
        }

        data class Failed(val message: String) : State
    }
}

class AudioRecorderHolder(val getOrNull: AudioRecorder?)

class AudioRecorderImpl(
    private val platformAudioRecorder: PlatformAudioRecorder,
    clock: Clock,
    private val parentScope: CoroutineScope,
    private val i18n: I18n,
) : AudioRecorder {
    private val stateImpl: MutableStateFlow<State> = MutableStateFlow(State.Ready)

    override val state: StateFlow<AudioRecorder.State> =
        stateImpl
            .emitRepeatedlyWhileRecording()
            .onEach { s -> s.onRecordingFailure { failure -> fail(failure) } }
            .map { it.toPublicState(clock) }
            .onEach { onMaxDuration(it) { complete() } }
            .stateIn(parentScope, SharingStarted.WhileSubscribed(), AudioRecorder.State.Ready)

    override suspend fun start(intoMediaStore: suspend (ByteArrayFlow) -> MediaReference) {
        closeSuspending()

        when (val startResult = platformAudioRecorder.start(intoMediaStore)) {
            is PlatformAudioRecorder.StartResult.Success ->
                stateImpl.value = withCatchCallbacks(startResult.startedRecording, i18n)

            is PlatformAudioRecorder.StartResult.Failure -> fail(State.Failed(startResult.message))

            PlatformAudioRecorder.StartResult.RequestedPermissions -> {
                delay(1.seconds) // wait for permission request to actually finish

                /**
                 * Reset the recorder so that a user can manually start another recording when he has allowed
                 * permission.
                 *
                 * @see PlatformAudioRecorder.StartResult.RequestedPermissions
                 */
                close()
            }
        }
    }

    override suspend fun load(state: AudioRecorder.State.Completed) {
        closeSuspending()

        platformAudioRecorder.load(state)?.let { stateImpl.value = it }
    }

    override suspend fun complete() {
        stateImpl.value = complete(stateImpl.value, i18n)
    }

    override suspend fun closeSuspending() {
        withContext(NonCancellable) {
            stateImpl.value = close(stateImpl.value, i18n)
            platformAudioRecorder.close()
        }
    }

    override fun close() {
        parentScope.launch { closeSuspending() }
    }

    private suspend fun fail(failure: State.Failed) {
        close(stateImpl.value, i18n)
        stateImpl.value = failure
    }

    /** Abstract effectful platform-specific actions by storing them here as function values */
    sealed interface State {
        object Ready : State

        data class Recording(
            val start: Instant,
            val loudness: () -> Float?,

            /**
             * Must write the recording into the media store and must guarantee clean up of all resources of the current
             * recording. The media store automatically deletes files if they are not used so this does not have to be
             * handled.
             */
            val complete: suspend () -> Result<Completed>,

            /**
             * Signal a failure from platform code to common code. Common code then sets the failure state. Platform
             * code is not concerned with setting the state directly.
             *
             * This is called repeatedly so should not have any side effects.
             */
            val failure: () -> Failed?,
        ) : State

        data class Completed(
            val capture: MediaReference,
            val duration: Duration,
            val sizeBytes: Long?,
            val contentType: ContentType,
            val fileExtension: String,
        ) : State

        data class Failed(val message: String) : State
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

        fun genericFailureOnError(i18n: I18n, registerOnErrorHandler: (() -> Unit) -> Unit): () -> State.Failed? {
            var failure: State.Failed? = null
            registerOnErrorHandler { failure = State.Failed(i18n.genericRecordingError()) }
            return { failure }
        }

        private suspend fun complete(stateImpl: State, i18n: I18n): State {
            return when (stateImpl) {
                State.Ready -> {
                    log.debug { "Tried to complete recording but it is not yet started" }
                    State.Ready
                }

                is State.Recording -> {
                    log.debug { "Completing recording" }

                    stateImpl
                        .complete()
                        .fold(
                            onSuccess = { state -> state },
                            onFailure = { t ->
                                log.debug { "Completing recording failed." }
                                State.Failed(t.message ?: i18n.genericRecordingError())
                            },
                        )
                }

                is State.Completed -> {
                    log.debug { "Tried to complete a recording that is already completed" }
                    stateImpl
                }
                is State.Failed -> {
                    log.debug { "Tried to complete a failed recording" }
                    stateImpl
                }
            }
        }

        private suspend fun close(stateImpl: State, i18n: I18n): State {
            log.debug { "Cleaning audio recorder" }
            complete(stateImpl, i18n)
            return State.Ready
        }

        /**
         * To achieve an immutable public API ([AudioRecorder.State]) we have to emit the original state
         * ([AudioRecorderImpl.State]) repeatedly and map it every time with updated data
         *
         * @see toPublicState
         */
        @OptIn(ExperimentalCoroutinesApi::class)
        private fun Flow<State>.emitRepeatedlyWhileRecording(): Flow<State> {
            return this.transformLatest { state ->
                when (state) {
                    is State.Recording ->
                        while (currentCoroutineContext().isActive) {
                            emit(state)
                            delay(50.milliseconds)
                        }

                    is State.Failed,
                    is State.Completed,
                    State.Ready -> {
                        emit(state)
                    }
                }
            }
        }

        private fun State.toPublicState(clock: Clock): AudioRecorder.State {
            return when (this) {
                is State.Recording -> {
                    AudioRecorder.State.Recording(duration = clock.now() - this.start, loudness = this.loudness() ?: 0f)
                }

                State.Ready -> AudioRecorder.State.Ready
                is State.Completed ->
                    AudioRecorder.State.Completed(
                        this.capture,
                        this.duration,
                        this.sizeBytes,
                        this.contentType,
                        this.fileExtension,
                    )

                is State.Failed -> AudioRecorder.State.Failed(this.message)
            }
        }

        private suspend fun onMaxDuration(state: AudioRecorder.State, callback: suspend () -> Unit) {
            when (state) {
                is AudioRecorder.State.Recording -> {
                    if (state.duration >= 5.hours) {
                        callback()
                    }
                }

                is AudioRecorder.State.Failed,
                is AudioRecorder.State.Completed,
                AudioRecorder.State.Ready -> Unit
            }
        }

        private suspend fun State.onRecordingFailure(callback: suspend (State.Failed) -> Unit) {
            when (this) {
                is State.Recording -> {
                    val failure = this.failure()
                    if (failure != null) {
                        callback(failure)
                    }
                }

                is State.Completed,
                is State.Failed,
                State.Ready -> Unit
            }
        }

        private fun withCatchCallbacks(recordingState: State.Recording, i18n: I18n): State.Recording {
            return recordingState.copy(
                loudness = {
                    try {
                        recordingState.loudness()
                    } catch (e: Throwable) {
                        log.debug(e) { "Getting audio loudness threw" }
                        null
                    }
                },
                complete = {
                    try {
                        recordingState.complete()
                    } catch (e: Throwable) {
                        log.warn(e) { "Completing audio recording threw." }
                        Result.failure(e)
                    }
                },
                failure = {
                    try {
                        recordingState.failure()
                    } catch (e: Throwable) {
                        log.error(e) { "Could not retrieve failure state" }
                        State.Failed(i18n.genericRecordingError())
                    }
                },
            )
        }
    }
}
