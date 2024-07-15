package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.messenger.util.FileTransferProgressElement
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.OpenModalCallback
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
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.clientserverapi.model.media.FileTransferProgress
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.utils.ByteArrayFlow
import org.koin.core.component.get


interface VideoMessageViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        timelineEvent: TimelineEvent?,
        content: RoomMessageEventContent.FileBased.Video,
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
        mediaUploadProgress: MutableStateFlow<FileTransferProgress?>
    ): VideoMessageViewModel {
        return VideoMessageViewModelImpl(
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
            mediaUploadProgress
        )
    }

    companion object : VideoMessageViewModelFactory
}

interface VideoMessageViewModel : FileBasedMessageViewModel {
    val thumbnail: StateFlow<ByteArray?>
    val width: Int
    val height: Int
    val duration: Int?
    fun getMaxHeight(): Int
    fun getHeight(maxWidth: Float): Int
    fun getWidth(maxWidth: Float, possibleHeight: Float): Int
    fun cancelThumbnailDownload()
    fun openVideo()
}

open class VideoMessageViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    timelineEvent: TimelineEvent?,
    private val content: RoomMessageEventContent.FileBased.Video,
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
    mediaUploadProgress: MutableStateFlow<FileTransferProgress?>
) : VideoMessageViewModel, AbstractFileBasedMessageViewModel(viewModelContext, content),
    MatrixClientViewModelContext by viewModelContext {
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
    override val width: Int = videoWidth(content)
    override val height: Int = videoHeight(content)
    override val duration: Int? = content.info?.duration
    override val uploadProgress: StateFlow<FileTransferProgressElement?> = thumbnails.mapProgressToProgressElement(mediaUploadProgress)
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

    private fun videoWidth(content: RoomMessageEventContent.FileBased.Video) =
        content.info?.width ?: 400

    private fun videoHeight(content: RoomMessageEventContent.FileBased.Video) =
        content.info?.height ?: 300
}

class PreviewVideoMessageViewModel : VideoMessageViewModel {
    override val thumbnail: MutableStateFlow<ByteArray?> = MutableStateFlow(previewImageByteArray())
    override val width: Int = 300
    override val height: Int = 200
    override val duration: Int = 950
    override val uploadProgress: MutableStateFlow<FileTransferProgressElement?> =
        MutableStateFlow(FileTransferProgressElement(0.3f, "145kB / 550kB"))

    override val fileSize: Int? = 200
    override val fileMimeType: String? = "video/mp4"

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
    override val downloadProgress: StateFlow<FileTransferProgressElement?> = MutableStateFlow(null)
    override val downloadSuccessful: StateFlow<Boolean> = MutableStateFlow(false)

    override val fileName: String = "video-123456789012345678901234567890.png"

    override fun downloadFile(): DownloadFile {
        return object : DownloadFile {
            override suspend fun getFileResult(): Result<ByteArrayFlow> =
                Result.success(flowOf(previewImageByteArray()))

            override suspend fun getFile(): ByteArrayFlow? = flowOf(previewImageByteArray())
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
    override val sender: StateFlow<UserInfoElement> =
        MutableStateFlow(UserInfoElement("Martin", UserId("martin:matrix.org")))
    override val formattedTime: String? = null
    override val invitation: StateFlow<String?> = MutableStateFlow(null)
    override val formattedDate: String = "23.11.21"
    override val showDateAbove: Boolean = false
}
