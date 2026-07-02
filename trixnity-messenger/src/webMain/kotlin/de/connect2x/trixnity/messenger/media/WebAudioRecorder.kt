package de.connect2x.trixnity.messenger.media

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.messenger.util.handleFirst
import io.ktor.http.*
import js.buffer.ArrayBuffer
import js.numbers.JsNumbers.toKotlinFloat
import js.objects.unsafeJso
import js.reflect.unsafeCast
import js.typedarrays.Float32Array
import kotlin.coroutines.resume
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.toJsArray
import kotlin.js.toList
import kotlin.math.absoluteValue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import web.audio.AnalyserNode
import web.audio.AudioContext
import web.blob.Blob
import web.events.ERROR
import web.events.Event
import web.events.EventHandler
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
    private val clock: Clock
) : PlatformAudioRecorder {
    private val log: Logger = Logger("de.connect2x.trixnity.messenger.media.WebAudioRecorder")

    @OptIn(ExperimentalWasmJsInterop::class)
    override suspend fun start(): AudioRecorderImpl.State.Recording? {
        val microphone =
            navigator.mediaDevices.getUserMedia(unsafeJso { audio = unsafeCast(true) })
        val recorder = startRecorder(microphone)
        return AudioRecorderImpl.State.Recording(
            start = clock.now(),
            loudness = loudness(microphone),
            complete = complete(recorder, microphone),
        )
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
    private fun complete(
        recorder: MediaRecorder,
        microphone: MediaStream
    ): suspend (AudioRecorderImpl.State.Recording) -> AudioRecorderImpl.State.Completed? {
        val chunks = mutableListOf<Blob>()
        val removeDataAvailableHandler =
            recorder.addEventHandler(
                type = BlobEvent.DATA_AVAILABLE,
                handler =
                    EventHandler { event ->
                        chunks += event.data
                    },
            )

        val opusContentType = ContentType.Audio.OGG.withParameter("codecs", "opus")
        return { recordingState: AudioRecorderImpl.State.Recording ->
            recorder.stop()
            val recording =
                withTimeoutOrNull(30.seconds) {
                    suspendCancellableCoroutine { cont ->
                        handleFirst(
                            eventTarget = recorder,
                            handlers =
                                mapOf(
                                    Event.STOP to
                                        {
                                            removeDataAvailableHandler()
                                            closeInputs(microphone)
                                            cont.resume(
                                                Blob(
                                                    chunks.toJsArray(),
                                                    unsafeJso { type = opusContentType.toString() },
                                                ),
                                            )
                                        },
                                    Event.ERROR to
                                        {
                                            removeDataAvailableHandler()
                                            closeInputs(microphone)
                                            cont.resume(null)
                                        },
                                ),
                        )
                    }
                }
            if (recording != null) {
                AudioRecorderImpl.State.Completed(
                    ReadOnlyBlobPlatformMedia(recording),
                    clock.now() - recordingState.start,
                    recording.size.toLong(),
                    opusContentType,
                ) {
                    // let garbage collector delete it
                }
            } else {
                null
            }
        }
    }

    private fun loudness(microphone: MediaStream): () -> Float? {
        val microphoneNode = audioContext.createMediaStreamSource(microphone)
        val analyser = AnalyserNode(audioContext)
        microphoneNode.connect(analyser)
        return {
            val pcmSamplesJs = Float32Array<ArrayBuffer>(analyser.frequencyBinCount)
            analyser.getFloatTimeDomainData(pcmSamplesJs)

            val pcmSamples = mutableListOf<Float>()
            pcmSamplesJs.forEach { pcmSamples.add(it.toKotlinFloat()) }
            val loudnessSamples = pcmSamples.map { it.absoluteValue }
            loudnessSamples.average().toFloat()
        }
    }

    override suspend fun load(state: AudioRecorder.State.Completed): AudioRecorderImpl.State.Completed {
        return AudioRecorderImpl.State.Completed(
            capture = state.data,
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
