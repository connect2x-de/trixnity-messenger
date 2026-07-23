package de.connect2x.trixnity.messenger.media

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.lognity.api.logger.error
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
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
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
import web.blob.byteArray
import web.errors.ERROR
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
    ): AudioRecorderImpl.State.Recording? {
        return try {
            val microphone =
                try {
                    // timeout if user neither denies nor allows microphone permission
                    withTimeoutOrNull(15.seconds) {
                        navigator.mediaDevices.getUserMedia(unsafeJso { audio = unsafeCast(true) })
                    }
                } catch (e: JsException) {
                    if (e.toJsErrorLike().toJsError().name == JsErrorName("NotAllowedError")) {
                        log.info { "Microphone permission denied" }
                    }
                    return null
                }
            if (microphone != null) {
                val recorder = startRecorder(microphone)
                val (media, mediaSize) = recordIntoMediaStore(recorder, intoMediaStore)
                val start = clock.now()
                AudioRecorderImpl.State.Recording(
                    start = start,
                    loudness = loudness(microphone),
                    complete = complete(recorder, microphone, media, mediaSize, start),
                    failure = genericFailureOnError(recorder),
                )
            } else {
                log.info { "Microphone permission request timed out." }
                null
            }
        } catch (e: Throwable) {
            log.error(e) { "Unexpected error. Could not start recording" }
            null
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

    private fun startRecorder(microphone: MediaStream): MediaRecorder {
        val recorder = MediaRecorder(microphone)
        recorder.start()
        return recorder
    }

    private fun recordIntoMediaStore(
        recorder: MediaRecorder,
        intoMediaStore: suspend (ByteArrayFlow) -> AudioRecorder.State.Completed.MediaReference,
    ): Pair<Deferred<AudioRecorder.State.Completed.MediaReference>, () -> Double> {
        var recordingSizeBytes = 0.0
        val chunks =
            callbackFlow {
                    val handlerRemovers =
                        listOf(
                            recorder.addEventHandler(
                                type = BlobEvent.DATA_AVAILABLE,
                                handler = { event ->
                                    recordingSizeBytes += event.data.size
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

        val media = coroutineScope.async { intoMediaStore(chunks) }
        return media to { recordingSizeBytes }
    }

    @OptIn(ExperimentalWasmJsInterop::class)
    private suspend fun complete(
        recorder: MediaRecorder,
        microphone: MediaStream,
        mediaDeferred: Deferred<AudioRecorder.State.Completed.MediaReference>,
        mediaSize: () -> Double,
        start: Instant,
    ): suspend () -> AudioRecorderImpl.State.Completed? {
        val opusContentType = ContentType.Audio.OGG.withParameter("codecs", "opus")
        val opusFileExtension = "ogg"
        return {
            try {
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
                    val media = mediaDeferred.await()
                    AudioRecorderImpl.State.Completed(
                        media,
                        clock.now() - start,
                        mediaSize().toLong(),
                        opusContentType,
                        opusFileExtension,
                    )
                } else {
                    null
                }
            } finally {
                mediaDeferred.cancel()
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

    override suspend fun load(state: AudioRecorder.State.Completed): AudioRecorderImpl.State.Completed {
        return AudioRecorderImpl.State.Completed(
            capture = state.media,
            duration = state.duration,
            sizeBytes = state.sizeBytes,
            contentType = state.contentType,
            fileExtension = state.fileExtension,
        )
    }

    override fun close() {
        // nothing to close
    }

    @OptIn(ExperimentalWasmJsInterop::class)
    private fun closeInputs(mediaStream: MediaStream) {
        mediaStream.getTracks().toList().forEach { track -> track.stop() }
    }
}
