package de.connect2x.trixnity.messenger.viewmodel.media

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.fleeksoft.io.ByteBuffer
import de.connect2x.trixnity.messenger.services.AudioPlayerService
import de.connect2x.trixnity.messenger.util.ActivityGetter
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.media.AudioPlayerViewModel.State
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.folivo.trixnity.client.media.okio.OkioPlatformMedia
import okio.Path
import org.koin.core.component.get
import java.nio.ByteOrder
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private val log = KotlinLogging.logger { }

class AndroidAudioPlayerViewModelFactory : AudioPlayerViewModelFactory {
    override fun create(
        viewModelContext: MatrixClientViewModelContext,
        audio: RoomMessageTimelineElementViewModel.FileBased.Audio
    ): AudioPlayerViewModel =
        AndroidAudioPlayerViewModel(
            viewModelContext = viewModelContext,
            audio = audio
        )
}

class AndroidAudioPlayerViewModel(
    viewModelContext: MatrixClientViewModelContext,
    private val audio: RoomMessageTimelineElementViewModel.FileBased.Audio
) : AudioPlayerViewModel, MatrixClientViewModelContext by viewModelContext {
    private val getActivity: ActivityGetter = get()
    private val tempFile: MutableStateFlow<OkioPlatformMedia.TemporaryFile?> = MutableStateFlow(null)
    private val stateChangeMutex: Mutex = Mutex()

    private var audioPlayerService: AudioPlayerService? = null
    private var playerServiceSynchronizationJob: Job? = null
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            startPlayerServiceSynchronizationJob()
            audioPlayerService = (service as AudioPlayerService.ServiceBinder).getService()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            audioPlayerService = null
            playerServiceSynchronizationJob?.cancel()
        }
    }

    override val elapsedTime: MutableStateFlow<Duration> = MutableStateFlow(Duration.ZERO)
    override val state: MutableStateFlow<State> = MutableStateFlow(State.Loading)

    init {
        lifecycle.doOnDestroy {
            get<CoroutineScope>().launch {
                log.debug { "Stopping playback and disposing resources" }
                // TODO: stopSuspending()
                tempFile.value?.delete()
            }
        }

        coroutineScope.launch {
            audio.downloadMedia { audioMedia ->
                check(audioMedia is OkioPlatformMedia) { "Audio media is expected to be OkioPlatformMedia" }
                audioMedia.getTemporaryFile().fold(
                    onFailure = {
                        log.error(it) { "Unexpected error while downloading media for audio player" }
                        state.value = State.Failed(it)
                    },
                    onSuccess = { file ->
                        log.debug { "Successfully downloaded audio media" }
                        tempFile.value = file
                        getNormalizedSamplesOfFile(file).fold(
                            onFailure = { // TODO: Should we do a fallback if this is not possible
                                log.error(it) { "Unexpected error when trying to acquire PCM samples of file" }
                            },
                            onSuccess = { amplitudes ->
                                log.trace { "Successfully extracted ${amplitudes.size} PCM samples out of file" }
                                state.value = State.Ready(amplitudes)
                            }
                        )
                    }
                )
            }
        }
    }

    // TODO: When we start playing audio A and start playing audio B, we want to stop audio A and start playing audio B
    override fun start() {
        coroutineScope.launch {
            stateChangeMutex.withLock {
                check(state.value is State.Ready && tempFile.value != null) { "Audio player is currently not ready" }
                val context = getActivity()

                // TODO: User should be able to skip to a position before playing
                val audioMediaUri = Uri.fromFile(requireNotNull(tempFile.value).path.toFile())
                val intent = Intent(context, AudioPlayerService::class.java)
                intent.action = AudioPlayerService.START_ACTION
                intent.putExtra(AudioPlayerService.START_AUDIO_URI, audioMediaUri)
                intent.putExtra(AudioPlayerService.MIME_TYPE, audio.mimeType)
                intent.putExtra(AudioPlayerService.POSITION, 0)

                log.info { "Start playing audio file" }
                ContextCompat.startForegroundService(context, intent)
                if (audioPlayerService == null) {
                    log.debug { "No bound audio player service found, attempting to bind to..." }
                    val intent = Intent(context, AudioPlayerService::class.java)
                    context.bindService(intent, serviceConnection, Context.BIND_IMPORTANT)
                }

                state.value = State.Playing(0F, (state.value as State.Ready).amplitudes)
            }
        }
    }

    // TODO: What is when exiting the room UI and change back into it. We should do some state recovery
    override fun stop() {
        coroutineScope.launch {
            stopSuspending()
        }
    }

    private suspend fun stopSuspending() {
        stateChangeMutex.withLock {
            check(state.value is State.Playing) { "Audio player is currently not playing" }

            val context = getActivity()
            if (audioPlayerService != null) {
                playerServiceSynchronizationJob?.cancel()
                context.unbindService(serviceConnection)
                audioPlayerService = null
            }

            state.value = State.Ready((state.value as State.Playing).amplitudes)
        }
    }

    // TODO: Reduce memory footprint by chunking the decoded PCM samples of the audio file and downsample them
    //       in-operation.
    private fun getNormalizedSamplesOfFile(file: OkioPlatformMedia.TemporaryFile): Result<MutableList<Float>> {
        val allSamples: MutableList<Float> = mutableListOf()
        val mediaExtractor = MediaExtractor()
        mediaExtractor.setDataSource(file.path.toString())

        var trackIndex = -1
        var trackFormat: MediaFormat? = null
        for (i in 0 until mediaExtractor.trackCount) { // TODO: Is this really a good idea
            val format = mediaExtractor.getTrackFormat(i)
            val mimeType = format.getString(MediaFormat.KEY_MIME)
            if (mimeType?.startsWith("audio/") == true) {
                trackIndex = i
                trackFormat = format
                break
            }
        }

        if (trackIndex == -1 || trackFormat == null) {
            return Result.failure(IllegalStateException("Unable to acquire audio track from audio file"))
        }

        mediaExtractor.selectTrack(trackIndex)
        val mimeType = requireNotNull(trackFormat.getString(MediaFormat.KEY_MIME))
        val mediaCodec = MediaCodec.createDecoderByType(mimeType)
        mediaCodec.configure(trackFormat, null, null, 0)
        mediaCodec.start()

        val bufferInfo = MediaCodec.BufferInfo()
        while (true) {
            val inputIndex = mediaCodec.dequeueInputBuffer(10_000)
            if (inputIndex >= 0) {
                val inputBuffer = requireNotNull(mediaCodec.getInputBuffer(inputIndex))
                val sampleSize = mediaExtractor.readSampleData(inputBuffer, 0)
                if (sampleSize < 0) {
                    mediaCodec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                } else {
                    mediaCodec.queueInputBuffer(inputIndex, 0, sampleSize, mediaExtractor.sampleTime, 0)
                    mediaExtractor.advance()
                }
            }

            val outputIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 10_000)
            if (outputIndex >= 0) {
                val outputBuffer = requireNotNull(mediaCodec.getOutputBuffer(outputIndex))
                val chunk = ByteArray(bufferInfo.size)
                outputBuffer.position(bufferInfo.offset)
                outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                outputBuffer.get(chunk)

                val shorts = ShortArray(chunk.size / 2)
                ByteBuffer.wrap(chunk)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .asShortBuffer()
                    .get(shorts)

                allSamples.addAll(shorts.map { it.toFloat() })
                mediaCodec.releaseOutputBuffer(outputIndex, false)

            }

            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                break
            }
        }

        mediaCodec.stop()
        mediaCodec.release()
        mediaExtractor.release()
        val maximum = allSamples.maxOfOrNull { kotlin.math.abs(it) } ?: 1f
        return Result.success(allSamples.map { it / maximum }.toMutableList())
    }

    private fun startPlayerServiceSynchronizationJob() {
        playerServiceSynchronizationJob?.cancel()
        playerServiceSynchronizationJob = coroutineScope.launch {
            audioPlayerService?.let { service ->
                launch {
                    combine(service.elapsedTime, service.duration) { elapsedTime, duration ->
                        elapsedTime.toFloat() / duration
                    }.collect { percentagePlayed ->
                        stateChangeMutex.withLock {
                            if (state.value is State.Playing) {
                                state.value = State.Playing(percentagePlayed, (state.value as State.Playing).amplitudes)
                            }
                        }
                    }

                    // service.elapsedTime.collect { currentPosition ->
                    //     elapsedTime.value = currentPosition.milliseconds
                    // }
                }
            }
        }
    }

}
