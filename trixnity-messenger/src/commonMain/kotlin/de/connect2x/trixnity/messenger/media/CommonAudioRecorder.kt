package de.connect2x.trixnity.messenger.media

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.lognity.api.logger.debug
import de.connect2x.lognity.api.logger.warn
import de.connect2x.trixnity.client.media.PlatformMedia
import de.connect2x.trixnity.messenger.util.ExperimentalTrixnityMessengerApi
import io.ktor.http.ContentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

/**
 * Use this with delegation to implement a platform-specific [AudioRecorder]
 */
// TODO: make this abstract and move function from [PlatformAudioRecorder] to here. [AndroidAudioRecorder] then implements this
@ExperimentalTrixnityMessengerApi
class CommonAudioRecorder(
    private val platformAudioRecorder: PlatformAudioRecorder,
    clock: Clock,
    parentScope: CoroutineScope,
): AudioRecorder {
    private val commonState: MutableStateFlow<CommonState> =
        MutableStateFlow(CommonState.Ready)

    override val state: StateFlow<AudioRecorder.State> =
        commonState.sampleToPublicState(clock)
            .onEach { onMaxDuration(it) { complete() } }
            .stateIn(parentScope, SharingStarted.WhileSubscribed(), AudioRecorder.State.Ready)

    override suspend fun startSuspending() {
        close()

        val initialRecordingState = platformAudioRecorder.start()
        if (initialRecordingState != null) {
            commonState.value = withCatchCallbacks(initialRecordingState)
        }
    }

    override fun complete() {
        commonState.value = complete(commonState.value)
    }

    override fun close() {
        platformAudioRecorder.close()
        commonState.value = close(commonState.value)
    }


    /**
     * Abstract effectful platform-specific actions by storing them here as function values
     */
    sealed interface CommonState {
        object Ready : CommonState
        data class Recording(
            val start: Instant,
            val loudness: () -> Float?,
            val complete: (Recording) -> Completed?,
        ) : CommonState
        data class Completed(
            val capture: PlatformMedia,
            val duration: Duration,
            val sizeBytes: Long?,
            val contentType: ContentType,
            val deleteCapture: () -> Unit
        ) : CommonState
    }

    data class Format<Container, Encoder>(
        val container: Container,
        val encoder: Encoder,
        val sampleRate: SampleRateHz,
        val contentType: ContentType,
    ) {
        companion object {
            val amrWbContentType = ContentType("audio", "amr-wb")
        }

        enum class SampleRateHz(val value: Int) {
            OPUS_SAMPLING_RATE_HZ(48_000),
            AMR_WB_SAMPLING_RATE_HZ(16_000)
        }
    }

    companion object {
        private val log: Logger = Logger("de.connect2x.trixnity.messenger.media.CommonAudioRecorder")

        private fun complete(commonState: CommonState): CommonState {
            return when (commonState) {
                CommonState.Ready -> {
                    log.debug { "Audio recorder not started" }
                    CommonState.Ready
                }

                is CommonState.Recording -> {
                    log.debug { "Stopping audio recorder" }

                    val completedState = try {
                        commonState.complete(commonState)
                    } catch (t: Throwable) {
                        log.warn(t) { "Completing audio recording failed." }
                        null
                    }
                    if (completedState != null) {
                        completedState
                    } else {
                        log.warn { "Stopping audio recorder failed" }
                        CommonState.Ready
                    }
                }

                is CommonState.Completed -> {
                    log.debug { "Audio recorder already stopped" }
                    commonState
                }
            }
        }

        private fun close(commonState: CommonState): CommonState {
            log.debug { "Cleaning audio recorder" }
            when (val completed = complete(commonState)) {
                is CommonState.Completed ->
                    try {
                        completed.deleteCapture()
                    } catch (t: Throwable) {
                        log.warn(t) { "Failed to close audio recorder" }
                    }
                CommonState.Ready -> Unit
                is CommonState.Recording -> Unit
            }

            return CommonState.Ready
        }

        /**
         * Sampling so that the public API ([AudioRecorder.State]) can be immutable
         *
         * TODO: test how often this code is run by mocking the time source and modifying the time manually
         */
        private fun Flow<CommonState>.sampleToPublicState(clock: Clock): Flow<AudioRecorder.State> {
            @OptIn(ExperimentalCoroutinesApi::class)
            fun emitRepeatedlyWhileRecording(commonState: Flow<CommonState>): Flow<CommonState> {
                return commonState.transformLatest { state ->
                    when (state) {
                        is CommonState.Recording ->
                            while (currentCoroutineContext().isActive) {
                                emit(state)
                                delay(50.milliseconds)
                            }
                        is CommonState.Completed, CommonState.Ready -> {
                            emit(state)
                        }
                    }
                }
            }

            fun toPublicState(commonState: CommonState): AudioRecorder.State {
                return when (commonState) {
                    is CommonState.Recording -> {
                        AudioRecorder.State.Recording(
                            duration = clock.now() - commonState.start,
                            loudness = commonState.loudness() ?: 0f
                        )
                    }

                    CommonState.Ready -> AudioRecorder.State.Ready
                    is CommonState.Completed ->
                        AudioRecorder.State.Completed(
                            commonState.capture,
                            commonState.duration,
                            commonState.sizeBytes,
                            commonState.contentType
                        )
                }
            }

            return emitRepeatedlyWhileRecording(this)
                .map { toPublicState(it) }
        }

        private fun onMaxDuration(state: AudioRecorder.State, callback: () -> Unit) {
            when (state) {
                is AudioRecorder.State.Recording -> {
                    if (state.duration >= 5.hours) {
                        callback()
                    }
                }
                is AudioRecorder.State.Completed, AudioRecorder.State.Ready ->
                    Unit
            }
        }

        private fun withCatchCallbacks(recordingState: CommonState.Recording): CommonState.Recording {
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
                }
            )
        }
    }
}
