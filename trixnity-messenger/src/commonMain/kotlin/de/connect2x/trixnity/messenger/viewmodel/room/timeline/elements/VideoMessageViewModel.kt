package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.files.FileTransferProgressElement
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.OpenModalType
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.SizeComputations
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.Thumbnails
import de.connect2x.trixnity.messenger.viewmodel.util.previewImageByteArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import net.folivo.trixnity.clientserverapi.model.media.FileTransferProgress
import net.folivo.trixnity.core.model.events.m.room.EncryptedFile
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import org.koin.core.component.get

interface VideoMessageViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        formattedDate: String,
        showDateAbove: Boolean,
        formattedTime: String?,
        isByMe: Boolean,
        showChatBubbleEdge: Boolean,
        showBigGap: Boolean,
        showSender: Flow<Boolean>,
        sender: Flow<UserInfoElement>,
        invitation: Flow<String?>,
        content: RoomMessageEventContent.FileBased.Video,
        onOpenModal: (type: OpenModalType, mxcUrl: String, encryptedFile: EncryptedFile?, fileName: String) -> Unit,
    ): VideoMessageViewModel {
        return VideoMessageViewModelImpl(
            viewModelContext,
            formattedDate,
            showDateAbove,
            formattedTime,
            isByMe,
            showChatBubbleEdge,
            showBigGap,
            showSender,
            sender,
            invitation,
            content,
            onOpenModal,
        )
    }

    companion object : VideoMessageViewModelFactory
}

interface VideoMessageViewModel : FileBasedMessageViewModel {
    val thumbnail: StateFlow<ByteArray?>
    val width: Int
    val height: Int
    val progress: StateFlow<FileTransferProgressElement?>
    fun getMaxHeight(): Int
    fun getHeight(maxWidth: Float): Int
    fun getWidth(maxWidth: Float, possibleHeight: Float): Int
    fun cancelThumbnailDownload()
    fun openVideo()
}

open class VideoMessageViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    override val formattedDate: String,
    override val showDateAbove: Boolean,
    override val formattedTime: String?,
    override val isByMe: Boolean,
    override val showChatBubbleEdge: Boolean,
    override val showBigGap: Boolean,
    showSender: Flow<Boolean>,
    sender: Flow<UserInfoElement>,
    invitation: Flow<String?>,
    private val content: RoomMessageEventContent.FileBased.Video,
    private val onOpenModal: (type: OpenModalType, mxcUrl: String, encryptedFile: EncryptedFile?, fileName: String) -> Unit,
) : VideoMessageViewModel, AbstractFileBasedMessageViewModel(viewModelContext, content),
    MatrixClientViewModelContext by viewModelContext {
    override val invitation: StateFlow<String?> =
        invitation.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
    override val sender: StateFlow<UserInfoElement> =
        sender.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), UserInfoElement(""))
    override val showSender: StateFlow<Boolean> =
        showSender.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), true)

    private val thumbnails = get<Thumbnails>()

    private val thumbnailProgressFlow = MutableStateFlow<FileTransferProgress?>(null)
    private val thumbnailLoad = getThumbnailAsync()

    override val thumbnail: StateFlow<ByteArray?> = channelFlow {
        send(thumbnailLoad.await())
    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
    override val width: Int = thumbnailWidth(content)
    override val height: Int = thumbnailHeight(content)
    override val progress: StateFlow<FileTransferProgressElement?> =
        thumbnails.mapProgressToProgressElement(thumbnailProgressFlow)
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    override fun getMaxHeight(): Int = 300

    override fun getHeight(maxWidth: Float) = SizeComputations.getHeight(height, getMaxHeight(), width, maxWidth)
    override fun getWidth(maxWidth: Float, possibleHeight: Float) =
        SizeComputations.getWidth(height, possibleHeight, width, maxWidth)

    override fun cancelThumbnailDownload() {
        thumbnailLoad.cancel()
    }

    override fun openVideo() {
        // if you have video working replace this with: 'url?.let { onOpenModal(OpenModalType.VIDEO, it, encryptedFile, getFileNameWithExtension()) }'
        openSaveFileDialog()
    }

    private fun getThumbnailAsync(): Deferred<ByteArray?> =
        coroutineScope.async {
            thumbnails.loadThumbnail(matrixClient, content, thumbnailProgressFlow)
        }

    private fun thumbnailWidth(content: RoomMessageEventContent.FileBased.Video) =
        content.info?.thumbnailInfo?.width ?: 400

    private fun thumbnailHeight(content: RoomMessageEventContent.FileBased.Video) =
        content.info?.thumbnailInfo?.height ?: 300
}

class PreviewVideoMessageViewModel : VideoMessageViewModel {
    override val thumbnail: MutableStateFlow<ByteArray?> = MutableStateFlow(previewImageByteArray())
    override val width: Int = 300
    override val height: Int = 200
    override val progress: MutableStateFlow<FileTransferProgressElement?> =
        MutableStateFlow(FileTransferProgressElement(0.3f, "145kB / 550kB"))

    override fun openVideo() {
    }

    override fun getMaxHeight(): Int {
        return 200
    }

    override fun getHeight(maxWidth: Float): Int {
        return 200
    }

    override fun getWidth(maxWidth: Float, possibleHeight: Float): Int {
        return 300
    }

    override fun cancelThumbnailDownload() {
    }

    override val saveFileDialogOpen: StateFlow<Boolean> = MutableStateFlow(false)
    override val downloadProgressElement: MutableStateFlow<StateFlow<FileTransferProgressElement?>?> =
        MutableStateFlow(null)
    override val downloadSuccessful: MutableStateFlow<StateFlow<Boolean>?> = MutableStateFlow(null)

    override val fileName: String = "video-123456789012345678901234567890.png"

    override fun downloadFile(): DownloadFile {
        return object : DownloadFile {
            override suspend fun getFileResult(): Result<ByteArray> = Result.success(previewImageByteArray())
            override suspend fun getFile(): ByteArray? = previewImageByteArray()
        }
    }

    override fun downloadFile(progressElement: MutableStateFlow<FileTransferProgressElement?>): DownloadFile {
        return object : DownloadFile {
            override suspend fun getFileResult(): Result<ByteArray> = Result.success(previewImageByteArray())
            override suspend fun getFile(): ByteArray? = previewImageByteArray()
        }
    }

    override fun cancelDownload() {
    }

    override fun getCoroutineContextForDownloadingFile(): CoroutineScope {
        return CoroutineScope(Dispatchers.Default)
    }

    override fun openSaveFileDialog() {
    }

    override fun closeSaveFileDialog() {
    }

    override val isByMe: Boolean = false
    override val showChatBubbleEdge: Boolean = false
    override val showBigGap: Boolean = false
    override val showSender: StateFlow<Boolean> = MutableStateFlow(true)
    override val sender: StateFlow<UserInfoElement> = MutableStateFlow(UserInfoElement("Martin"))
    override val formattedTime: String? = null
    override val invitation: StateFlow<String?> = MutableStateFlow(null)
    override val formattedDate: String = "23.11.21"
    override val showDateAbove: Boolean = false

}