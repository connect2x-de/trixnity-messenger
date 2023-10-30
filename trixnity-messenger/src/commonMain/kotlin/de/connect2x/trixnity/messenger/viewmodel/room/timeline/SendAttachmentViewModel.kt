package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import com.arkivanov.essenty.backhandler.BackCallback
import de.connect2x.trixnity.messenger.MessengerConfig
import de.connect2x.trixnity.messenger.util.getImageDimensions
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.util.MimeTypes.guessByFileName
import de.connect2x.trixnity.messenger.viewmodel.util.formatSize
import de.connect2x.trixnity.messenger.viewmodel.util.previewImageByteArray
import io.github.oshai.kotlinlogging.KotlinLogging
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
import net.folivo.trixnity.utils.toByteArrayFlow
import okio.buffer

private val log = KotlinLogging.logger { }

interface SendAttachmentViewModelFactory {
    fun newSendAttachmentViewModel(
        viewModelContext: MatrixClientViewModelContext,
        file: String,
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
    val fileName: String
    val fileSize: StateFlow<String>
    val byteArray: ByteArray

    fun isImage(): Boolean
    fun isVideo(): Boolean
    fun isAudio(): Boolean
    fun send()
    fun cancel()
}

open class SendAttachmentViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    file: String,
    private val selectedRoomId: RoomId,
    private val onCloseAttachmentSendView: () -> Unit,
) : MatrixClientViewModelContext by viewModelContext, SendAttachmentViewModel {

    private val _error: MutableStateFlow<String?> = MutableStateFlow(null)
    private val fileInfo = getFileInfo(file)
    private val _sendEnabled = MutableStateFlow(_error.value == null)

    override val error: StateFlow<String?> = _error.asStateFlow()
    override val sendEnabled: StateFlow<Boolean> = _sendEnabled.asStateFlow()
    override val fileName = fileInfo.fileName
    override val fileSize = MutableStateFlow("")
    override val byteArray: ByteArray = fileInfo.source.buffer().readByteArray()

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

    override fun isImage(): Boolean {
        return guessByFileName(fileName).match("image/*")
    }

    override fun isVideo(): Boolean {
        return guessByFileName(fileName).match("video/*")
    }

    override fun isAudio(): Boolean {
        return guessByFileName(fileName).match("audio/*")
    }

    override fun send() {
        _sendEnabled.value = false
        coroutineScope.launch {
            matrixClient.room.sendMessage(selectedRoomId) {
                when {
                    isImage() -> {
                        val (width, height) = getImageDimensions(byteArray)
                        image(
                            body = fileName,
                            image = byteArray.toByteArrayFlow(),
                            type = guessByFileName(fileName),
                            size = fileInfo.fileSize?.toInt(),
                            width = width,
                            height = height,
                        )
                    }

                    isVideo() -> video(
                        body = fileName,
                        video = byteArray.toByteArrayFlow(),
                        type = guessByFileName(fileName),
                        size = fileInfo.fileSize?.toInt(),
                    ) // TODO width, height, duration

                    isAudio() -> {
                        audio(
                            body = fileName,
                            audio = byteArray.toByteArrayFlow(),
                            type = guessByFileName(fileName),
                            size = fileInfo.fileSize?.toInt(),
                        ) // TODO duration
                    }

                    else -> file(
                        body = fileName,
                        file = byteArray.toByteArrayFlow(),
                        type = guessByFileName(fileName),
                        name = fileName,
                        size = fileInfo.fileSize?.toInt()
                    )
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
    override val fileName: String = "anImage.png"
    override val fileSize: MutableStateFlow<String> = MutableStateFlow("1337 KB")
    override val byteArray: ByteArray = previewImageByteArray()

    override fun isImage(): Boolean {
        return true
    }

    override fun isVideo(): Boolean {
        return false
    }

    override fun isAudio(): Boolean {
        return false
    }

    override fun send() {
    }

    override fun cancel() {
    }

}
