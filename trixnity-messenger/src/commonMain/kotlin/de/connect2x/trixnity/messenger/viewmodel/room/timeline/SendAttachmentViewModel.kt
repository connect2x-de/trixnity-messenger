package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import com.arkivanov.essenty.backhandler.BackCallback
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.messenger.util.getImageDimensions
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.util.formatSize
import de.connect2x.trixnity.messenger.viewmodel.util.previewImageByteArray
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.message.audio
import net.folivo.trixnity.client.room.message.file
import net.folivo.trixnity.client.room.message.image
import net.folivo.trixnity.client.room.message.video
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.utils.toByteArray
import org.koin.core.component.get

private val log = KotlinLogging.logger { }

interface SendAttachmentViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        file: FileDescriptor,
        selectedRoomId: RoomId,
        onCloseAttachmentSendView: () -> Unit,
    ): SendAttachmentViewModel {
        return SendAttachmentViewModelImpl(
            viewModelContext, file, selectedRoomId, onCloseAttachmentSendView
        )
    }

    companion object : SendAttachmentViewModelFactory
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

class SendAttachmentViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val file: FileDescriptor,
    private val selectedRoomId: RoomId,
    private val onCloseAttachmentSendView: () -> Unit,
) : MatrixClientViewModelContext by viewModelContext, SendAttachmentViewModel {

    private val messengerConfiguration = get<MatrixMessengerConfiguration>()
    private val _error: MutableStateFlow<String?> = MutableStateFlow(null)

    private val _sendEnabled = MutableStateFlow(_error.value == null)

    override val error: StateFlow<String?> = _error.asStateFlow()
    override val sendEnabled: StateFlow<Boolean> = _sendEnabled.asStateFlow()
    override val fileName = MutableStateFlow(file.fileName)
    override val fileSize = MutableStateFlow("(" + (file.fileSize?.let { size -> formatSize(size.toLong()) } ?: i18n.commonUnknown()) + ")")
    override val byteArray = file.content.map {
        it
    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    override val isImage = MutableStateFlow(file.mimeType?.match("image/*"))
    override val isVideo = MutableStateFlow(file.mimeType?.match("video/*"))
    override val isAudio = MutableStateFlow(file.mimeType?.match("audio/*"))

    private val backCallback = BackCallback {
        cancel()
    }

    init {
        backHandler.register(backCallback)
        coroutineScope.launch {
            val computedFileSize = file.fileSize
            if ((computedFileSize?.compareTo(messengerConfiguration.attachmentMaxSize * 1_000_000)
                    ?: 0) > 0
            ) {
                _error.value =
                    i18n.attachmentSizeMaxSizeError(messengerConfiguration.attachmentMaxSize)
            }
        }
    }

    override fun send() {
        _sendEnabled.value = false
        coroutineScope.launch {
            matrixClient.room.sendMessage(selectedRoomId) {
                val byteArrayFlow = file.content
                when {
                    isImage.value ?: false -> {
                        log.debug { "send an image" }
                        val (width, height) = getImageDimensions(byteArrayFlow.toByteArray())
                        image(
                            body = file.fileName,
                            fileName = file.fileName,
                            image = byteArrayFlow,
                            type = file.mimeType,
                            size = file.fileSize,
                            width = width,
                            height = height,
                        )
                    }

                    isVideo.value ?: false -> {
                        log.debug { "send a video" }
                        video(
                            body = file.fileName,
                            fileName = file.fileName,
                            video = byteArrayFlow,
                            type = file.mimeType,
                            size = file.fileSize,
                        )
                    } // TODO width, height, duration

                    isAudio.value ?: false -> {
                        log.debug { "send an audio" }
                        audio(
                            body = file.fileName,
                            fileName = file.fileName,
                            audio = byteArrayFlow,
                            type = file.mimeType,
                            size = file.fileSize,
                        ) // TODO duration
                    }

                    else -> {
                        log.debug { "send a file" }
                        file(
                            body = file.fileName,
                            file = byteArrayFlow,
                            type = file.mimeType,
                            fileName = file.fileName,
                            size = file.fileSize
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
