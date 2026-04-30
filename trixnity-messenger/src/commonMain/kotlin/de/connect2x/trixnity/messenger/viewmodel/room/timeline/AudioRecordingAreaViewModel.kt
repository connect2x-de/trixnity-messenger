package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import com.arkivanov.essenty.lifecycle.doOnDestroy
import de.connect2x.trixnity.core.model.events.m.room.RoomMessageEventContent
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import de.connect2x.trixnity.messenger.media.AudioRecorder
import de.connect2x.trixnity.messenger.media.AudioRecorderHolder
import de.connect2x.trixnity.messenger.util.DownloadManager
import de.connect2x.trixnity.messenger.util.getOrNull
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.media.AudioRecorderViewModel
import de.connect2x.trixnity.messenger.viewmodel.media.AudioRecorderViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.media.MediaPlayerViewModel
import de.connect2x.trixnity.messenger.viewmodel.media.MediaPlayerViewModelFactory
import io.ktor.http.ContentType
import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.get
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@TrixnityMessengerPrivateApi
interface AudioRecordingAreaViewModel {
    val recorder: AudioRecorderViewModel?
    val capturePlayer: StateFlow<MediaPlayerViewModel?>

    fun sendAudioMessage()

    fun deleteAudioMessage()

    fun loadAudioMessage(content: RoomMessageEventContent.FileBased.Audio)
}

interface AudioRecordingAreaViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        sendAudioMessage: () -> Unit,
        deleteAudioDraftMessage: suspend () -> Unit
    ): AudioRecordingAreaViewModel {
        return AudioRecordingAreaViewModelImpl(
            viewModelContext,
            sendAudioMessage,
            deleteAudioDraftMessage
        )
    }

    companion object : AudioRecordingAreaViewModelFactory
}

class AudioRecordingAreaViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val _sendAudioMessage: () -> Unit,
    private val deleteAudioDraftMessage: suspend () -> Unit
) : MatrixClientViewModelContext by viewModelContext, AudioRecordingAreaViewModel {

    private val downloadManager = viewModelContext.get<DownloadManager>()
    private val enableMessageDrafts = get<MatrixMessengerConfiguration>().features.enableMessageDrafts

    override val recorder: AudioRecorderViewModel? = run {
        val recorderHolder = getOrNull<AudioRecorderHolder>()
        val recorder = recorderHolder?.getOrNull
        if (recorder != null) {
            AudioRecorderViewModelImpl(viewModelContext, recorder)
        } else {
            null
        }
    }

    override val capturePlayer: StateFlow<MediaPlayerViewModel?> = run {
        fun create(capture: AudioRecorder.State.Completed): MediaPlayerViewModel? {
            return getOrNull<MediaPlayerViewModelFactory>()
                ?.create(
                    id = "AudioRecordingAreaViewModel",
                    viewModelContext = viewModelContext,
                    mimeType = capture.contentType.toString(),
                    initialDuration = Duration.ZERO,
                    acquireFile = { Result.success(capture.data) },
                )
        }

        fun replaceOnCapture(recorderState: Flow<AudioRecorder.State>): StateFlow<MediaPlayerViewModel?> {
            return recorderState
                .map { state ->
                    when (state) {
                        AudioRecorder.State.Ready,
                        is AudioRecorder.State.Recording -> {
                            capturePlayer.value?.pause()
                            null
                        }
                        is AudioRecorder.State.Completed -> {
                            val newPlayer = create(state)
                            lifecycle.doOnDestroy {
                                newPlayer?.close()
                                log.error { "Destroyed" }
                            }
                            newPlayer
                        }
                    }
                }
                .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
        }

        if (recorder != null) {
            replaceOnCapture(recorder.state)
        } else {
            MutableStateFlow(null)
        }
    }

    override fun sendAudioMessage() {
        _sendAudioMessage()
    }

    override fun deleteAudioMessage() {
        if (enableMessageDrafts) {
            coroutineScope.launch {
                deleteAudioDraftMessage()
            }
        }
        recorder?.close()
    }

    override fun loadAudioMessage(content: RoomMessageEventContent.FileBased.Audio) {
        coroutineScope.launch {
            val duration = content.info?.duration

            if (duration != null) {
                val data = downloadManager.startDownloadAsync(
                    matrixClient,
                    content,
                    content.fileName ?: content.body,
                    MutableStateFlow(null)
                )

                data.await().onSuccess {
                    val type = content.info?.mimeType?.dropLastWhile { char -> char != '/' }?.dropLast(1)
                    val subtype = content.info?.mimeType?.dropWhile { char -> char != '/' }?.drop(1)
                    val contentType = if (type?.isNotEmpty() == true && subtype?.isNotEmpty() == true) {
                        ContentType(type, subtype)
                    } else {
                        ContentType.Audio.OGG
                    }

                    recorder?.loadSuspending(
                        AudioRecorder.State.Completed(
                            data = it,
                            duration = duration.milliseconds,
                            sizeBytes = content.info?.size,
                            contentType = contentType
                        )
                    )
                }.onFailure {
                    log.warn { "Failed downloading audio message draft" }
                }
            } else {
                log.warn { "Failed loading audio message draft, because the duration was null" }
            }
        }
    }
}

class PreviewAudioRecordingAreaViewModel : AudioRecordingAreaViewModel {
    override val recorder: AudioRecorderViewModel? = null
    override val capturePlayer: StateFlow<MediaPlayerViewModel?> = MutableStateFlow(null)

    override fun sendAudioMessage() = Unit
    override fun deleteAudioMessage() = Unit
    override fun loadAudioMessage(content: RoomMessageEventContent.FileBased.Audio) = Unit
}
