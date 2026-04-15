package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import com.arkivanov.essenty.lifecycle.doOnDestroy
import de.connect2x.trixnity.client.room.message.MessageBuilder
import de.connect2x.trixnity.client.room.message.audio
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.messenger.media.AudioRecorder
import de.connect2x.trixnity.messenger.media.AudioRecorderHolder
import de.connect2x.trixnity.messenger.util.ExperimentalTrixnityMessengerApi
import de.connect2x.trixnity.messenger.util.getOrNull
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.media.AudioRecorderViewModel
import de.connect2x.trixnity.messenger.viewmodel.media.AudioRecorderViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.media.MediaPlayerViewModel
import de.connect2x.trixnity.messenger.viewmodel.media.MediaPlayerViewModelFactory
import io.ktor.http.ContentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Duration

interface AudioRecordingAreaViewModel {
    val recorder: AudioRecorderViewModel?
    val capturePlayer: StateFlow<MediaPlayerViewModel?>

    fun sendAudioMessage()
}

interface AudioRecordingAreaViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        sendAudioMessage: (suspend MessageBuilder.() -> Unit) -> Unit,
    ): AudioRecordingAreaViewModel {
        return AudioRecordingAreaViewModelImpl(
            viewModelContext,
            sendAudioMessage
        )
    }

    companion object : AudioRecordingAreaViewModelFactory
}

@OptIn(ExperimentalTrixnityMessengerApi::class)
class AudioRecordingAreaViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val sendAudioMessage: (suspend MessageBuilder.() -> Unit) -> Unit,
) : MatrixClientViewModelContext by viewModelContext, AudioRecordingAreaViewModel {

    override val recorder: AudioRecorderViewModel? = run {
        val recorderHolder = getOrNull<AudioRecorderHolder>()
        val recorder = recorderHolder?.getOrNull
        if(recorder != null) {
            AudioRecorderViewModelImpl(viewModelContext, recorder)
        } else {
            null
        }
    }

    override val capturePlayer: StateFlow<MediaPlayerViewModel?> = run {
        fun create(capture: AudioRecorder.State.Completed?): MediaPlayerViewModel? {
            return if (capture != null) {
                getOrNull<MediaPlayerViewModelFactory>()?.create(
                    id = "AudioRecordingAreaViewModel",
                    viewModelContext = viewModelContext,
                    mimeType = capture.contentType.toString(),
                    initialDuration = Duration.ZERO,
                    acquireFile = { Result.success(capture.data) }
                )
            } else {
                null
            }
        }

        fun replaceOnCapture(recorderState: Flow<AudioRecorder.State>): StateFlow<MediaPlayerViewModel?> {
            return recorderState
                .map { state ->
                        when (state) {
                            AudioRecorder.State.Ready, is AudioRecorder.State.Recording -> {
                                capturePlayer.value?.pause()
                                null
                            }
                            is AudioRecorder.State.Completed -> {
                                val newPlayer = create(state)
                                lifecycle.doOnDestroy {
                                    newPlayer?.close()
                                    log.error{ "Destroyed" }
                                }
                                newPlayer
                            }
                        }
                }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
        }

        if (recorder != null) {
            replaceOnCapture(recorder.state)
        } else {
            MutableStateFlow(null)
        }
    }

    override fun sendAudioMessage() {
        val message: (suspend MessageBuilder.() -> Unit)? = when (val audioRecorderStateValue = recorder?.state?.value) {
            AudioRecorder.State.Ready -> { null }
            is AudioRecorder.State.Recording -> { null }
            is AudioRecorder.State.Completed -> { {
                    audio(
                        "voice message",
                        audioRecorderStateValue.data,
                        type = ContentType.Audio.OGG,
                        duration = audioRecorderStateValue.duration.inWholeMilliseconds,
                        size = audioRecorderStateValue.sizeBytes
                    )
                }
            }
            null -> { null }
        }
        if (message != null) {
            sendAudioMessage(message)
        }

    }
}

class PreviewAudioRecordingAreaViewModel: AudioRecordingAreaViewModel {
    override val recorder: AudioRecorderViewModel? = null
    override val capturePlayer: StateFlow<MediaPlayerViewModel?> = MutableStateFlow(null)
    override fun sendAudioMessage() = Unit
}
