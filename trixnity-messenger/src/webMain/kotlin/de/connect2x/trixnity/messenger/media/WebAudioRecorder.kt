package de.connect2x.trixnity.messenger.media

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.lognity.api.logger.error
import de.connect2x.lognity.api.logger.warn
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.util.handleFirst
import de.connect2x.trixnity.utils.ByteArrayFlow
import io.ktor.http.*
import js.array.asList
import js.buffer.ArrayBuffer
import js.errors.JsErrorName
import js.errors.name
import js.errors.toJsError
import js.errors.toJsErrorLike
import js.numbers.JsNumbers.toKotlinDouble
import js.numbers.JsNumbers.toKotlinFloat
import js.objects.unsafeJso
import js.reflect.unsafeCast
import js.typedarrays.Float32Array
import kotlin.coroutines.resume
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsException
import kotlin.js.toList
import kotlin.math.absoluteValue
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import web.audio.AnalyserNode
import web.audio.AudioContext
import web.blob.Blob
import web.blob.byteArray
import web.errors.DOMException
import web.errors.NotAllowedError
import web.errors.NotFoundError
import web.errors.NotReadableError
import web.events.ERROR
import web.events.Event
import web.events.STOP
import web.events.addEventHandler
import web.mediadevices.getUserMedia
import web.mediarecorder.BlobEvent
import web.mediarecorder.DATA_AVAILABLE
import web.mediarecorder.MediaRecorder
import web.mediastreams.MediaStream
import web.navigator.navigator
import web.performance.performance

class WebAudioRecorder(
    private val audioContext: AudioContext,
    private val clock: Clock,
    private val coroutineScope: CoroutineScope,
    private val i18n: I18n,
) : PlatformAudioRecorder {
    private val log: Logger = Logger("de.connect2x.trixnity.messenger.media.WebAudioRecorder")

    @OptIn(ExperimentalWasmJsInterop::class)
    override suspend fun start(
        intoMediaStore: suspend (ByteArrayFlow) -> AudioRecorder.State.Completed.MediaReference
    ): PlatformAudioRecorder.StartResult {
        return try {
            val microphone =
                try {
                    // timeout if user neither denies nor allows microphone permission
                    withTimeoutOrNull(15.seconds) {
                        navigator.mediaDevices.getUserMedia(unsafeJso { audio = unsafeCast(true) })
                    }
                } catch (e: JsException) {
                    val missingBrowserPermissionAny = DOMException.NotAllowedError
                    val missingSystemPermissionFirefox = DOMException.NotFoundError
                    val missingSystemPermissionChromium = DOMException.NotReadableError
                    val missingSystemPermissionEpiphanyWebKit = JsErrorName("OverconstrainedError")

                    /**
                     * Because the documentation is very vague, we reproduced missing system permissions (e.g. native
                     * macOS permissions) manually and inspected which errors are thrown. For this, we installed the
                     * browser using Flatpak and used Flatseal to revoke the 'pulseaudio' permission.
                     *
                     * See also this list of all possible exceptions:
                     * https://developer.mozilla.org/en-US/docs/Web/API/MediaDevices/getUserMedia#exceptions
                     */
                    val permissionErrors =
                        listOf(
                            missingBrowserPermissionAny,
                            missingSystemPermissionFirefox,
                            missingSystemPermissionChromium,
                            missingSystemPermissionEpiphanyWebKit,
                        )

                    if (permissionErrors.contains(e.name())) {
                        log.warn(e) {
                            "Microphone access not possible. Either permission denied (by system or user) or microphone otherwise unavailable"
                        }
                        return PlatformAudioRecorder.StartResult.Failure(i18n.microphonePermissionDeniedWeb())
                    }
                    throw e
                }
            if (microphone != null) {
                val recorder = MediaRecorder(microphone)
                val timing = RecordingTiming(performance.now().toKotlinDouble())
                val removeTimingHandler =
                    recorder.addEventHandler(BlobEvent.DATA_AVAILABLE) { event ->
                        timing.update(event.timeStamp.toKotlinDouble())
                    }
                val recording = RecordingResult(recorder, intoMediaStore, coroutineScope)
                recorder.start(1000)
                val start = clock.now()
                PlatformAudioRecorder.StartResult.Success(
                    AudioRecorderImpl.State.Recording(
                        start = start,
                        loudness = loudness(microphone),
                        complete = complete(recorder, microphone, recording, timing, removeTimingHandler),
                        failure = genericFailureOnError(recorder),
                    )
                )
            } else {
                PlatformAudioRecorder.StartResult.Failure(i18n.microphonePermissionTimeout())
            }
        } catch (e: Throwable) {
            log.error(e) { "Unexpected error. Could not start recording" }
            PlatformAudioRecorder.StartResult.Failure(i18n.genericRecordingErrorWeb())
        }
    }

    private fun genericFailureOnError(recorder: MediaRecorder): () -> AudioRecorderImpl.State.Failed? =
        AudioRecorderImpl.genericFailureOnError(i18n) { setFailure ->
            recorder.addEventHandler(
                type = Event.ERROR,
                options = unsafeJso { once = true },
                handler = {
                    log.error { "Unexpected error while recording audio" }
                    setFailure()
                },
            )
        }

    private class RecordingResult(
        recorder: MediaRecorder,
        intoMediaStore: suspend (ByteArrayFlow) -> AudioRecorder.State.Completed.MediaReference,
        coroutineScope: CoroutineScope,
    ) {
        var sizeBytes: Double = 0.0
            private set

        private var mimeType: String = ""

        val contentType: ContentType
            get() = ContentType.parse(mimeType)

        val fileExtension: String?
            get() = contentType.fileExtensions().firstOrNull()

        private fun addChunk(blob: Blob) {
            sizeBytes += blob.size
            // stop() resets recorder.mimeType; use the type of the recorded blobs instead.
            if (blob.type.isNotBlank()) mimeType = blob.type
        }

        private val chunks =
            callbackFlow {
                    val handlerRemovers =
                        listOf(
                            recorder.addEventHandler(
                                type = BlobEvent.DATA_AVAILABLE,
                                handler = { event ->
                                    addChunk(event.data)
                                    trySend(event.data)
                                },
                            ),
                            recorder.addEventHandler(
                                type = Event.ERROR,
                                handler = { event ->
                                    close(IllegalStateException("Unexpected error while recording audio"))
                                },
                            ),
                            recorder.addEventHandler(type = Event.STOP, handler = { event -> close() }),
                        )
                    awaitClose { handlerRemovers.forEach { it() } }
                }
                .buffer(UNLIMITED)
                .map { it.byteArray() }

        val media: Deferred<AudioRecorder.State.Completed.MediaReference> = coroutineScope.async {
            intoMediaStore(chunks)
        }
    }

    @OptIn(ExperimentalWasmJsInterop::class)
    private suspend fun complete(
        recorder: MediaRecorder,
        microphone: MediaStream,
        recording: RecordingResult,
        timing: RecordingTiming,
        removeTimingHandler: () -> Unit,
    ): suspend () -> Result<AudioRecorderImpl.State.Completed> {
        return {
            try {
                timing.stop(performance.now().toKotlinDouble())
                recorder.stop()
                val recordingSuccessful =
                    withTimeoutOrNull(5.seconds) {
                        suspendCancellableCoroutine { cont ->
                            handleFirst(
                                eventTarget = recorder,
                                handlers =
                                    mapOf(Event.STOP to { cont.resume(Unit) }, Event.ERROR to { cont.resume(null) }),
                            )
                        }
                    }
                if (recordingSuccessful != null) {
                    val duration = timing.duration
                    val media = recording.media.await()
                    val fileExtension =
                        recording.fileExtension
                            ?: run {
                                log.warn {
                                    "No file extension for recording content type ${recording.contentType}; using ogg"
                                }
                                "ogg"
                            }
                    Result.success(
                        AudioRecorderImpl.State.Completed(
                            media,
                            duration,
                            recording.sizeBytes.toLong(),
                            recording.contentType,
                            fileExtension,
                        )
                    )
                } else {
                    log.warn { "Stopping the web API recorder failed or timed out" }
                    Result.failure(Throwable(i18n.genericRecordingErrorWeb()))
                }
            } finally {
                removeTimingHandler()
                recording.media.cancel()
                closeInputs(microphone)
            }
        }
    }

    private fun loudness(microphone: MediaStream): () -> Float? {
        val analyser = analyserOf(microphone)
        return { loudnessSamples(analyser).average().toFloat() }
    }

    private fun loudnessSamples(analyser: AnalyserNode): List<Float> {
        return pcmSamples(analyser).map { it.absoluteValue }
    }

    /** PCM can be negative because it models a full audio wave */
    private fun pcmSamples(analyser: AnalyserNode): List<Float> {
        val samples = Float32Array<ArrayBuffer>(analyser.frequencyBinCount)
        analyser.getFloatTimeDomainData(samples)
        return samples.asList().map { it.toKotlinFloat() }
    }

    private fun analyserOf(mediaStream: MediaStream): AnalyserNode {
        val input = audioContext.createMediaStreamSource(mediaStream)
        val analyser = AnalyserNode(audioContext)
        input.connect(analyser)
        return analyser
    }

    override fun close() {
        // nothing to close
    }

    @OptIn(ExperimentalWasmJsInterop::class)
    private fun closeInputs(mediaStream: MediaStream) {
        mediaStream.getTracks().toList().forEach { track -> track.stop() }
    }

    @OptIn(ExperimentalWasmJsInterop::class)
    private fun JsException.name(): JsErrorName {
        return this.toJsErrorLike().toJsError().name
    }
}

/** Event timestamps share performance.now()'s monotonic clock; chunk delivery intervals can vary. */
internal class RecordingTiming(private val startTimestamp: Double) {
    private var latestTimestamp = startTimestamp
    private var stopTimestamp: Double? = null

    val duration: Duration
        get() =
            (latestTimestamp.coerceAtMost(stopTimestamp ?: latestTimestamp) - startTimestamp)
                .coerceAtLeast(0.0)
                .milliseconds

    fun update(timestamp: Double) {
        latestTimestamp = maxOf(latestTimestamp, timestamp)
    }

    fun stop(timestamp: Double) {
        stopTimestamp = timestamp
    }
}
