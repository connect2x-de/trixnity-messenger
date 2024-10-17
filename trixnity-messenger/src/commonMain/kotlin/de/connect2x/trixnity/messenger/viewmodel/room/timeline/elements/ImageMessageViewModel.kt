package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.messenger.util.FileTransferProgressElement
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.files.MediaConstants.MAX_SIZE_IMAGE_PREVIEW_BYTES
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.OpenModalCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.OpenModalType
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.SizeComputations
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.Thumbnails
import de.connect2x.trixnity.messenger.viewmodel.util.previewImageByteArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.clientserverapi.model.media.FileTransferProgress
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.utils.ByteArrayFlow
import org.koin.core.component.get


interface ImageMessageViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        timelineEvent: TimelineEvent?,
        content: RoomMessageEventContent.FileBased.Image,
        formattedDate: String,
        showDateAbove: Boolean,
        formattedTime: String?,
        isByMe: Boolean,
        showChatBubbleEdge: Boolean,
        showBigGap: Boolean,
        showSender: Flow<Boolean>,
        sender: Flow<UserInfoElement>,
        invitation: Flow<String?>,
        onOpenModal: OpenModalCallback,
        mediaUploadProgress: MutableStateFlow<FileTransferProgress?>,
    ): ImageMessageViewModel = ImageMessageViewModelImpl(
        viewModelContext,
        timelineEvent,
        content,
        formattedDate,
        showDateAbove,
        formattedTime,
        isByMe,
        showChatBubbleEdge,
        showBigGap,
        showSender,
        sender,
        invitation,
        onOpenModal,
        mediaUploadProgress,
    )

    companion object : ImageMessageViewModelFactory
}

interface ImageMessageViewModel : FileBasedMessageViewModel {
    val thumbnail: StateFlow<ByteArray?>
    val width: Int
    val height: Int

    fun openImage()
    fun getMaxHeight(): Int
    fun getHeight(maxWidth: Float): Int
    fun getWidth(maxWidth: Float, possibleHeight: Float): Int
    fun cancelThumbnailDownload()
}

class ImageMessageViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    timelineEvent: TimelineEvent?,
    private val content: RoomMessageEventContent.FileBased.Image,
    override val formattedDate: String,
    override val showDateAbove: Boolean,
    override val formattedTime: String?,
    override val isByMe: Boolean,
    override val showChatBubbleEdge: Boolean,
    override val showBigGap: Boolean,
    showSender: Flow<Boolean>,
    sender: Flow<UserInfoElement>,
    invitation: Flow<String?>,
    private val onOpenModal: OpenModalCallback,
    mediaUploadProgress: MutableStateFlow<FileTransferProgress?>,
) : ImageMessageViewModel, AbstractFileBasedMessageViewModel(viewModelContext, content, onOpenModal),
    MatrixClientViewModelContext by viewModelContext {
    override val showReactions: StateFlow<Boolean> = MutableStateFlow(true)
    override val invitation: StateFlow<String?> =
        invitation.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
    override val sender: StateFlow<UserInfoElement> =
        sender.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), UserInfoElement("", UserId("")))
    override val showSender: StateFlow<Boolean> =
        showSender.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), true)

    private val thumbnails = get<Thumbnails>()

    private val thumbnailProgressFlow = MutableStateFlow<FileTransferProgress?>(null)
    private val thumbnailLoad = getThumbnailAsync()

    override val thumbnail: StateFlow<ByteArray?> = channelFlow {
        send(thumbnailLoad.await())
    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    override val width: Int = imageWidth(content)
    override val height: Int = imageHeight(content)
    override val uploadProgress: StateFlow<FileTransferProgressElement?> =
        if (mediaUploadProgress.value != null) {
            thumbnails.mapProgressToProgressElement(mediaUploadProgress)
        } else {
            thumbnails.mapProgressToProgressElement(thumbnailProgressFlow)
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    override fun getMaxHeight(): Int = 300

    override fun getHeight(maxWidth: Float) = SizeComputations.getHeight(height, getMaxHeight(), width, maxWidth)
    override fun getWidth(maxWidth: Float, possibleHeight: Float) =
        SizeComputations.getWidth(height, possibleHeight, width, maxWidth)

    override fun openImage() {
        if ((fileSize ?: 0) > MAX_SIZE_IMAGE_PREVIEW_BYTES) {
            openSaveFileDialog()
        } else url?.let { onOpenModal(OpenModalType.IMAGE, it, encryptedFile, fileName) }
    }

    override fun cancelThumbnailDownload() {
        thumbnailLoad.cancel()
    }

    private fun getThumbnailAsync(): Deferred<ByteArray?> =
        coroutineScope.async {
            thumbnails.loadThumbnail(matrixClient, content, thumbnailProgressFlow)
        }


    private fun imageWidth(content: RoomMessageEventContent.FileBased.Image) =
        content.info?.width ?: 400

    private fun imageHeight(content: RoomMessageEventContent.FileBased.Image) =
        content.info?.height ?: 300
}

class PreviewImageMessageViewModel : ImageMessageViewModel {
    override val showReactions: StateFlow<Boolean> = MutableStateFlow(true)
    override val thumbnail: MutableStateFlow<ByteArray?> = MutableStateFlow(previewImageByteArray())
    override val width: Int = 300
    override val height: Int = 200
    override val uploadProgress: MutableStateFlow<FileTransferProgressElement?> =
        MutableStateFlow(FileTransferProgressElement(0.3f, "145kB / 550kB"))

    override fun openImage() {
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
    override val downloadProgress: StateFlow<FileTransferProgressElement?> = MutableStateFlow(null)
    override val downloadSuccessful: StateFlow<Boolean> = MutableStateFlow(false)
    override val downloadError: MutableStateFlow<String?> = MutableStateFlow(null)
    override val fileName: String = "image-1234567890123456678901234567890.jpg"
    override val fileSize: Long = 200
    override val fileMimeType: String = "image/jpg"

    override fun downloadFile(onFile: suspend (ByteArrayFlow) -> Unit) {
        CoroutineScope(Dispatchers.Default).launch {
            onFile(flowOf(previewImageByteArray()))
        }
    }

    override fun cancelDownload() {
    }

    override fun openSaveFileDialog() {
    }

    override fun closeSaveFileDialog() {
    }

    override val isByMe: Boolean = false
    override val showChatBubbleEdge: Boolean = false
    override val showBigGap: Boolean = false
    override val showSender: StateFlow<Boolean> = MutableStateFlow(true)
    override val sender: StateFlow<UserInfoElement> =
        MutableStateFlow(UserInfoElement("Martin", UserId("martin:matrix.org")))
    override val formattedTime: String? = null
    override val invitation: StateFlow<String?> = MutableStateFlow(null)
    override val formattedDate: String = "23.11.21"
    override val showDateAbove: Boolean = false
}
