package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import com.arkivanov.essenty.backhandler.BackCallback
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.messenger.util.ManualFileDescriptor
import de.connect2x.trixnity.messenger.util.ProcessImageUpload
import de.connect2x.trixnity.messenger.util.getImageDimensions
import de.connect2x.trixnity.messenger.util.rotateImageToMetadataOrientation
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.util.checkFileSizeExceedsLimit
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.ContentType.Image
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.message.audio
import net.folivo.trixnity.client.room.message.file
import net.folivo.trixnity.client.room.message.image
import net.folivo.trixnity.client.room.message.video
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.utils.ByteArrayFlow
import net.folivo.trixnity.utils.byteArrayFlowFromSource
import net.folivo.trixnity.utils.toByteArray
import net.folivo.trixnity.utils.toByteArrayFlow
import okio.Buffer
import org.koin.core.component.get


private val log = KotlinLogging.logger { }
private const val MAX_IMAGE_FILE_SIZE_MB_TO_CHECK = 500

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
    val file: FileDescriptor
    val isImage: Boolean?
    val isVideo: Boolean?
    val isAudio: Boolean?
    val fileContent: StateFlow<ByteArrayFlow?>

    fun send()
    fun cancel()
}

class SendAttachmentViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    override val file: FileDescriptor,
    private val selectedRoomId: RoomId,
    private val onCloseAttachmentSendView: () -> Unit,
) : MatrixClientViewModelContext by viewModelContext, SendAttachmentViewModel {

    private val messengerConfiguration = get<MatrixMessengerConfiguration>()
    private val _error: MutableStateFlow<String?> = MutableStateFlow(null)

    private val _sendEnabled = MutableStateFlow(_error.value == null)

    override val error: StateFlow<String?> = _error.asStateFlow()
    override val sendEnabled: StateFlow<Boolean> = _sendEnabled.asStateFlow()

    override val isImage = file.mimeType?.match("image/*")
    override val isVideo = file.mimeType?.match("video/*")
    override val isAudio = file.mimeType?.match("audio/*")

    private val _fileContent = MutableStateFlow<ByteArrayFlow?>(null)
    override val fileContent: StateFlow<ByteArrayFlow?> = _fileContent.asStateFlow()

    private val backCallback = BackCallback {
        cancel()
    }

    init {
        backHandler.register(backCallback)
        coroutineScope.launch {
            if (checkFileSizeExceedsLimit(
                    fileSize = file.fileSize,
                    maxSizeMB = messengerConfiguration.attachmentMaxSize
                )
            ) {
                _error.value = i18n.attachmentSizeMaxSizeError(messengerConfiguration.attachmentMaxSize)
            }
            _sendEnabled.value = _error.value == null
            _fileContent.value = if (isImage == true && !checkFileSizeExceedsLimit(
                    file.fileSize,
                    MAX_IMAGE_FILE_SIZE_MB_TO_CHECK
                )
            ) get<ProcessImageUpload>().invoke(file.content.toByteArray(), file.mimeType ?: Image.PNG).toByteArrayFlow()
            else {
                file.content
            }
        }
    }

    override fun send() {
        if (_sendEnabled.value) {
            _sendEnabled.value = false
            coroutineScope.launch {
                matrixClient.room.sendMessage(selectedRoomId) {
                    val byteArrayFlow = file.content
                    when {
                        isImage ?: false -> {
                            log.debug { "send an image" }
                            val (width, height) = getImageDimensions(byteArrayFlow)
                            image(
                                body = file.fileName,
                                fileName = file.fileName,
                                image = fileContent.value ?: file.content,
                                type = file.mimeType,
                                size = file.fileSize,
                                width = width,
                                height = height,
                            )
                        }

                        isVideo ?: false -> {
                            log.debug { "send a video" }
                            video(
                                body = file.fileName,
                                fileName = file.fileName,
                                video = byteArrayFlow,
                                type = file.mimeType,
                                size = file.fileSize,
                            )
                        } // TODO width, height, duration

                        isAudio ?: false -> {
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
    }

    override fun cancel() {
        onCloseAttachmentSendView()
    }

}

class PreviewSendAttachmentViewModel() : SendAttachmentViewModel {
    override val error: MutableStateFlow<String?> = MutableStateFlow(null)
    override val sendEnabled: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val file: FileDescriptor = ManualFileDescriptor(
        fileName = "",
        fileSize = null,
        mimeType = null,
        content = byteArrayFlowFromSource { Buffer() })
    override val isImage: Boolean = true
    override val isVideo: Boolean = false
    override val isAudio: Boolean = false
    override val fileContent: StateFlow<ByteArrayFlow?> = MutableStateFlow(null)

    override fun send() {
    }

    override fun cancel() {
    }
}
