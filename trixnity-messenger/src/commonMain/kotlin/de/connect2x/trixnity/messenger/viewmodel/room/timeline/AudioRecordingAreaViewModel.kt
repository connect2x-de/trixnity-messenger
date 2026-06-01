package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import com.arkivanov.essenty.lifecycle.doOnDestroy
import de.connect2x.trixnity.client.room
import de.connect2x.trixnity.client.room.message.MessageBuilder
import de.connect2x.trixnity.client.room.message.audio
import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomId
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
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.component.get

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
        roomId: RoomId,
        currentReply: MutableStateFlow<Pair<RoomId, EventId>?>,
        resetCurrentReply: () -> Unit,
        draftMutex: Mutex,
    ): AudioRecordingAreaViewModel {
        return AudioRecordingAreaViewModelImpl(viewModelContext, roomId, currentReply, resetCurrentReply, draftMutex)
    }

    companion object : AudioRecordingAreaViewModelFactory
}

const val fallbackAudioFileExtension = "m4a"

class AudioRecordingAreaViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val roomId: RoomId,
    private val currentReply: MutableStateFlow<Pair<RoomId, EventId>?>,
    private val resetCurrentReply: () -> Unit,
    private val draftMutex: Mutex,
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

    init {
        coroutineScope.launch {
            if (enableMessageDrafts) {
                recorder?.state?.collect { state ->
                    when (state) {
                        is AudioRecorder.State.Completed -> draftMutex.withLock { saveAudioAsDraft() }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun getAudioMessageBuilder(): (suspend MessageBuilder.() -> Unit)? {
        val repliedEvent = currentReply.value

        val audioMessage: (suspend MessageBuilder.() -> Unit)? =
            when (val audioRecorderStateValue = recorder?.state?.value) {
                AudioRecorder.State.Ready -> null

                is AudioRecorder.State.Recording -> null

                is AudioRecorder.State.Completed -> {
                    {
                        audio(
                            "",
                            audioRecorderStateValue.data,
                            fileName = "voice_message.${audioRecorderStateValue.fileExtension}",
                            type = audioRecorderStateValue.contentType,
                            duration = audioRecorderStateValue.duration.inWholeMilliseconds,
                            size = audioRecorderStateValue.sizeBytes,
                        )
                    }
                }

                null -> null
            }

        return audioMessage?.let {
            {
                when {
                    repliedEvent != null -> {
                        reply(repliedEvent)
                    }
                }
                audioMessage()
            }
        }
    }

    private suspend fun MessageBuilder.reply(repliedEvent: Pair<RoomId, EventId>) {
        reply(matrixClient, repliedEvent)
    }

    private suspend fun saveAudioAsDraft(): Boolean {
        val builder = getAudioMessageBuilder() ?: return false
        matrixClient.room.setDraftMessage(roomId = roomId, builder = builder)
        return true
    }

    override fun sendAudioMessage() {
        log.trace { "try to send audio message" }
        coroutineScope.launch {
            if (enableMessageDrafts) {
                draftMutex.withLock {
                    if (saveAudioAsDraft()) {
                        matrixClient.room.sendDraftMessage(roomId)
                    }
                }
            } else {
                getAudioMessageBuilder()?.let { matrixClient.room.sendMessage(roomId = roomId, builder = it) }
            }
            recorder?.close()
            resetCurrentReply()
        }
    }

    override fun deleteAudioMessage() {
        if (enableMessageDrafts) {
            coroutineScope.launch { matrixClient.room.deleteDraftMessage(roomId) }
        }
        recorder?.close()
    }

    override fun loadAudioMessage(content: RoomMessageEventContent.FileBased.Audio) {
        coroutineScope.launch {
            val duration = content.info?.duration

            if (duration != null) {
                val data =
                    downloadManager.startDownloadAsync(
                        matrixClient,
                        content,
                        content.fileName ?: content.body,
                        MutableStateFlow(null),
                    )

                data
                    .await()
                    .onSuccess {
                        val contentType =
                            content.info?.mimeType?.let { mimeType -> ContentType.parse(mimeType) }
                                ?: ContentType.Audio.MP4
                        val fileExtension = content.fileName?.substringAfterLast(".") ?: fallbackAudioFileExtension

                        recorder?.loadSuspending(
                            AudioRecorder.State.Completed(
                                data = it,
                                duration = duration.milliseconds,
                                sizeBytes = content.info?.size,
                                contentType = contentType,
                                fileExtension = fileExtension,
                            )
                        )
                    }
                    .onFailure { log.warn { "Failed downloading audio message draft" } }
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
