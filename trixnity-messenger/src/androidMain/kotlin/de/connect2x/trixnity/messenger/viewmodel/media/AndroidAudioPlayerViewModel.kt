package de.connect2x.trixnity.messenger.viewmodel.media

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import androidx.core.content.ContextCompat
import de.connect2x.trixnity.messenger.services.AudioPlayerService
import de.connect2x.trixnity.messenger.util.ActivityGetter
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.media.AudioPlayerViewModel.State
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.folivo.trixnity.client.media.okio.OkioPlatformMedia
import okio.Path
import org.koin.core.component.get
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
    private val tempFile: MutableStateFlow<Path?> = MutableStateFlow(null)
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
                        tempFile.value = file.path
                        state.value = State.Ready
                    }
                )
            }
        }
    }

    // TODO: When we start playing audio A and start playing audio B, we want to stop audio A and start playing audio B
    override fun start() {
        coroutineScope.launch {
            stateChangeMutex.withLock {
                check(state.value is State.Ready || tempFile.value == null) { "Audio player is currently not ready" }
                val context = getActivity()

                // TODO: User should be able to skip to a position before playing
                val audioMediaUri = Uri.fromFile(requireNotNull(tempFile.value).toFile())
                val intent = Intent(context, AudioPlayerService::class.java)
                intent.action = AudioPlayerService.START_ACTION
                intent.putExtra(AudioPlayerService.START_AUDIO_URI, audioMediaUri)
                intent.putExtra(AudioPlayerService.MIME_TYPE, audio.mimeType)
                intent.putExtra(AudioPlayerService.POSITION, 0)
                ContextCompat.startForegroundService(requireNotNull(context), intent)
                if (audioPlayerService == null) {
                    log.debug { "No bound audio player service found, attempting to bind to..." }
                    val intent = Intent(context, AudioPlayerService::class.java)
                    context.bindService(intent, serviceConnection, Context.BIND_IMPORTANT)
                }
                state.value = State.Playing
            }
        }
    }

    // TODO: What is when exiting the room UI and change back into it. We should do some state recovery
    override fun stop() {
        coroutineScope.launch {
            stateChangeMutex.withLock {
                check(state.value is State.Playing) { "Audio player is currently not playing" }

                val context = getActivity()
                if (audioPlayerService != null) {
                    playerServiceSynchronizationJob?.cancel()
                    requireNotNull(context).unbindService(serviceConnection)
                    audioPlayerService = null
                }

                state.value = State.Ready
            }
        }
    }

    private fun startPlayerServiceSynchronizationJob() {
        playerServiceSynchronizationJob?.cancel()
        playerServiceSynchronizationJob = coroutineScope.launch {
            audioPlayerService?.let { service ->
                launch {
                    service.elapsedTime.collect { currentPosition ->
                        elapsedTime.value = currentPosition.milliseconds // TODO
                    }
                }
            }
        }
    }

}
