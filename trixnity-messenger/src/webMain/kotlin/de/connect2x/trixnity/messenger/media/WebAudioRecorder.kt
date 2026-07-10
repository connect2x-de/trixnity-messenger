package de.connect2x.trixnity.messenger.media

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.lognity.api.logger.error
import de.connect2x.trixnity.messenger.util.handleFirst
import de.connect2x.trixnity.utils.ByteArrayFlow
import io.ktor.http.*
import js.array.asList
import js.buffer.ArrayBuffer
import js.numbers.JsNumbers.toKotlinFloat
import js.objects.unsafeJso
import js.reflect.unsafeCast
import js.typedarrays.Float32Array
import kotlin.coroutines.resume
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.toList
import kotlin.math.absoluteValue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
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
) : PlatformAudioRecorder {
    private val log: Logger = Logger("de.connect2x.trixnity.messenger.media.WebAudioRecorder")

    @OptIn(ExperimentalWasmJsInterop::class)
    override suspend fun start(
        intoMediaStore: suspend (ByteArrayFlow) -> AudioRecorder.State.Completed.MediaReference,
    ): AudioRecorderImpl.State.Recording? {
        return try {
            val microphone =
                // timeout if user neither denies nor allows microphone permission
                withTimeoutOrNull(15.seconds) {
                    navigator.mediaDevices.getUserMedia(unsafeJso { audio = unsafeCast(true) })
                }
            if (microphone != null) {
                val recorder = startRecorder(microphone)
                AudioRecorderImpl.State.Recording(
                    start = clock.now(),
                    loudness = loudness(microphone),
                    complete = complete(recorder, microphone, intoMediaStore),
                )
            } else {
                log.info { "Microphone permission request timed out." }
                null
            }
        } catch (e: Exception) {
            log.error(e) { "Could not start recording" }
            null
        }
    }

    private fun startRecorder(microphone: MediaStream): MediaRecorder {
        val recorder = MediaRecorder(microphone)
        recorder.start()
        recorder.addEventHandler(
            type = Event.ERROR,
            handler = {
                log.error { "Error while recording audio" }
            },
        )
        return recorder
    }

    @OptIn(ExperimentalWasmJsInterop::class)
    private suspend fun complete(
        recorder: MediaRecorder,
        microphone: MediaStream,
        intoMediaStore: suspend (ByteArrayFlow) -> AudioRecorder.State.Completed.MediaReference,
    ): suspend (AudioRecorderImpl.State.Recording) -> AudioRecorderImpl.State.Completed? {
        var recordingSizeBytes = 0.0
        val chunks = callbackFlow {
            val handlerRemovers = listOf(
                recorder.addEventHandler(
                    type = BlobEvent.DATA_AVAILABLE,
                    handler =
                        { event ->
                            recordingSizeBytes += event.data.size
                            trySend(event.data)
                        },
                ),
                recorder.addEventHandler(
                    type = Event.ERROR,
                    handler =
                        { event ->
                            log.error { "Unexpected error while recording audio" }
                            close(IllegalStateException("Unexpected error while recording audio"))
                        },
                ),
                recorder.addEventHandler(
                    type = Event.STOP,
                    handler =
                        { event ->
                            close()
                        },
                )
            )
            awaitClose {
                handlerRemovers.forEach { it() }
            }
        }.buffer(UNLIMITED)
            .map { it.byteArray()}

        val mediaDeferred = coroutineScope.async {
            intoMediaStore(chunks)
        }

        val opusContentType = ContentType.Audio.OGG.withParameter("codecs", "opus")
        return { recordingState: AudioRecorderImpl.State.Recording ->
            recorder.stop()
            val recordingSuccessful =
                withTimeoutOrNull(30.seconds) {
                    suspendCancellableCoroutine { cont ->
                        handleFirst(
                            eventTarget = recorder,
                            handlers =
                                mapOf(
                                    Event.STOP to
                                        {
                                            closeInputs(microphone)
                                            cont.resume(
                                                Unit
                                            )
                                        },
                                    Event.ERROR to
                                        {
                                            closeInputs(microphone)
                                            log.error { "Unexpected error while recording audio" }
                                            cont.resume(null)
                                        },
                                ),
                        )
                    }
                }
            if (recordingSuccessful != null) {
                val media = mediaDeferred.await()
                AudioRecorderImpl.State.Completed(
                        media,
                        clock.now() - recordingState.start,
                        recordingSizeBytes.toLong(),
                        opusContentType,
                    ) {
                        // Automatically deleted by media store
                    }
            } else {
                null
            }
        }
    }

    private fun loudness(microphone: MediaStream): () -> Float? {
        val analyser = analyserOf(microphone)
        return {
            loudnessSamples(analyser).average().toFloat()
        }
    }

    private fun loudnessSamples(analyser: AnalyserNode): List<Float> {
        return pcmSamples(analyser).map { it.absoluteValue }
    }

    /**
     * PCM can be negative because it models a full audio wave
     */
    private fun pcmSamples(analyser: AnalyserNode): List<Float> {
        val samples = Float32Array<ArrayBuffer>(analyser.frequencyBinCount)
        analyser.getFloatTimeDomainData(samples)
        return samples
            .asList()
            .map { it.toKotlinFloat() }
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
        ) {}
    }

    override fun close() {
        // nothing to close
    }

    @OptIn(ExperimentalWasmJsInterop::class)
    private fun closeInputs(mediaStream: MediaStream) {
        mediaStream.getTracks().toList().forEach { track -> track.stop()}
    }
}
