package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import com.arkivanov.essenty.backhandler.BackCallback
import de.connect2x.trixnity.messenger.MessengerConfig
import de.connect2x.trixnity.messenger.util.getImageDimensions
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.util.formatSize
import de.connect2x.trixnity.messenger.viewmodel.util.previewImageByteArray
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.message.audio
import net.folivo.trixnity.client.room.message.file
import net.folivo.trixnity.client.room.message.image
import net.folivo.trixnity.client.room.message.video
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.utils.toByteArray

private val log = KotlinLogging.logger { }

interface SendAttachmentViewModelFactory {
    fun newSendAttachmentViewModel(
        viewModelContext: MatrixClientViewModelContext,
        file: FileDescriptor,
        selectedRoomId: RoomId,
        onCloseAttachmentSendView: () -> Unit,
    ): SendAttachmentViewModel {
        return SendAttachmentViewModelImpl(
            viewModelContext, file, selectedRoomId, onCloseAttachmentSendView
        )
    }
}

interface SendAttachmentViewModel {
    val error: StateFlow<String?>
    val sendEnabled: StateFlow<Boolean>
    val fileName: StateFlow<String?>
    val fileSize: StateFlow<String?>
    val byteArray: StateFlow<ByteArray?>

    val isImage: StateFlow<Boolean?>
    val isVideo: StateFlow<Boolean?>
    val isAudio: StateFlow<Boolean?>

    fun send()
    fun cancel()
}

open class SendAttachmentViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    file: FileDescriptor,
    private val selectedRoomId: RoomId,
    private val onCloseAttachmentSendView: () -> Unit,
) : MatrixClientViewModelContext by viewModelContext, SendAttachmentViewModel {

    private val _error: MutableStateFlow<String?> = MutableStateFlow(null)
    private val fileInfo: SharedFlow<FileInfo> = flow { emit(getFileInfo(file)) }
        .shareIn(coroutineScope, started = SharingStarted.Eagerly, replay = 1)
    private val _sendEnabled = MutableStateFlow(_error.value == null)

    override val error: StateFlow<String?> = _error.asStateFlow()
    override val sendEnabled: StateFlow<Boolean> = _sendEnabled.asStateFlow()
    override val fileName = fileInfo.map { it.fileName }
        .stateIn(coroutineScope, SharingStarted.Eagerly, null)
    override val fileSize =
        fileInfo.map { it?.let { "(" + (it.fileSize?.let { formatSize(it) } ?: i18n.commonUnknown()) + ")" } }
            .stateIn(coroutineScope, SharingStarted.Eagerly, null)
    override val byteArray = fileInfo.map { it.byteArrayFlow.toByteArray() }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    override val isImage = fileInfo.map { it.mimeType.match("image/*") }
        .stateIn(coroutineScope, SharingStarted.Eagerly, null)

    override val isVideo = fileInfo.map { it.mimeType.match("video/*") }
        .stateIn(coroutineScope, SharingStarted.Eagerly, null)

    override val isAudio = fileInfo.map { it.mimeType.match("audio/*") }
        .stateIn(coroutineScope, SharingStarted.Eagerly, null)

    private val backCallback = BackCallback {
        cancel()
    }

    init {
        backHandler.register(backCallback)
        val computedFileSize = fileInfo.fileSize
        if ((computedFileSize?.compareTo(MessengerConfig.instance.attachmentMaxSize * 1_000_000) ?: 0) > 0) {
            _error.value = i18n.attachmentSizeMaxSizeError(MessengerConfig.instance.attachmentMaxSize)
        }
        fileSize.value = " (" + (computedFileSize?.let { formatSize(it) } ?: i18n.commonUnknown()) + ")"
    }

    override fun send() {
        _sendEnabled.value = false
        coroutineScope.launch {
            matrixClient.room.sendMessage(selectedRoomId) {
                val fileInfo = fileInfo.first()
                val byteArrayFlow = fileInfo.byteArrayFlow
                when {
                    isImage.value ?: false -> {
                        log.debug { "send an image" }
                        val (width, height) = getImageDimensions(byteArrayFlow.toByteArray())
                        image(
                            body = fileInfo.fileName,
                            image = byteArrayFlow,
                            type = fileInfo.mimeType,
                            size = fileInfo.fileSize?.toInt(),
                            width = width,
                            height = height,
                        )
                    }

                    isVideo.value ?: false -> {
                        log.debug { "send a video" }
                        video(
                            body = fileInfo.fileName,
                            video = byteArrayFlow,
                            type = fileInfo.mimeType,
                            size = fileInfo.fileSize?.toInt(),
                        )
                    } // TODO width, height, duration

                    isAudio.value ?: false -> {
                        log.debug { "send an audio" }
                        audio(
                            body = fileInfo.fileName,
                            audio = byteArrayFlow,
                            type = fileInfo.mimeType,
                            size = fileInfo.fileSize?.toInt(),
                        ) // TODO duration
                    }

                    else -> {
                        log.debug { "send a file" }
                        file(
                            body = fileInfo.fileName,
                            file = byteArrayFlow,
                            type = fileInfo.mimeType,
                            name = fileInfo.fileName,
                            size = fileInfo.fileSize?.toInt()
                        )
                    }
                }
            }
            onCloseAttachmentSendView()
            _sendEnabled.value = error.value == null
        }
    }

    override fun cancel() {
        onCloseAttachmentSendView()
    }

}

class PreviewSendAttachmentViewModel() : SendAttachmentViewModel {
    override val error: MutableStateFlow<String?> = MutableStateFlow(null)
    override val sendEnabled: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val fileName: MutableStateFlow<String?> = MutableStateFlow("anImage.png")
    override val fileSize: MutableStateFlow<String> = MutableStateFlow("1337 KB")
    override val byteArray: MutableStateFlow<ByteArray?> = MutableStateFlow(previewImageByteArray())
    override val isImage: MutableStateFlow<Boolean?> = MutableStateFlow(true)
    override val isVideo: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val isAudio: MutableStateFlow<Boolean> = MutableStateFlow(false)

    override fun send() {
    }

    override fun cancel() {
    }
}
