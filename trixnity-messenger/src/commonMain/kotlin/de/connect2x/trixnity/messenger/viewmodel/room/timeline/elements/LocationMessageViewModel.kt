package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.messenger.util.FileTransferProgressElement
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.OpenModalType
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.SizeComputations
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.Thumbnails
import de.connect2x.trixnity.messenger.viewmodel.util.previewImageByteArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.clientserverapi.model.media.FileTransferProgress
import net.folivo.trixnity.core.model.events.m.room.EncryptedFile
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import org.koin.core.component.get

interface LocationMessageViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        timelineEvent: TimelineEvent?,
        content: RoomMessageEventContent.FileBased.Image, // needs change in core
        formattedDate: String,
        showDateAbove: Boolean,
        formattedTime: String?,
        isByMe: Boolean,
        showChatBubbleEdge: Boolean,
        showBigGap: Boolean,
        showSender: Flow<Boolean>,
        sender: Flow<UserInfoElement>,
        invitation: Flow<String?>,
        onOpenModal: (type: OpenModalType, mxcUrl: String, encryptedFile: EncryptedFile?, fileName: String) -> Unit,
        geoUri: String,
    ): LocationMessageViewModel {
        return LocationMessageViewModelImpl(
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
            geoUri,
            name = content.body
        )
    }

    companion object : LocationMessageViewModelFactory
}

interface LocationMessageViewModel {
    val thumbnail: StateFlow<ByteArray?>
    val width: Int
    val height: Int
    val geoUri: String
    val name: String

    fun getMaxHeight(): Int
    fun getHeight(maxWidth: Float): Int
    fun getWidth(maxWidth: Float, possibleHeight: Float): Int
    fun cancelThumbnailDownload()
}

class LocationMessageViewModelImpl(
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
    private val onOpenModal: (type: OpenModalType, mxcUrl: String, encryptedFile: EncryptedFile?, fileName: String) -> Unit,
    override val geoUri: String,
    override val name: String
) : LocationMessageViewModel, RoomMessageViewModel, MatrixClientViewModelContext by viewModelContext {
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

    override fun getMaxHeight(): Int = 300
    override fun getHeight(maxWidth: Float) = SizeComputations.getHeight(height, getMaxHeight(), width, maxWidth)
    override fun getWidth(maxWidth: Float, possibleHeight: Float) =
        SizeComputations.getWidth(height, possibleHeight, width, maxWidth)

    override fun cancelThumbnailDownload() {
        thumbnailLoad.cancel()
    }

    private fun getThumbnailAsync(): Deferred<ByteArray?> =
        coroutineScope.async {
            thumbnails.loadThumbnail(matrixClient, content, thumbnailProgressFlow)
        }


    private fun thumbnailWidth(content: RoomMessageEventContent.FileBased.Image) =
        content.info?.thumbnailInfo?.width ?: 400

    private fun thumbnailHeight(content: RoomMessageEventContent.FileBased.Image) =
        content.info?.thumbnailInfo?.height ?: 300
}

class PreviewLocationMessageViewModel() : LocationMessageViewModel {
    override val thumbnail: MutableStateFlow<ByteArray?> = MutableStateFlow(previewImageByteArray())
    override val width: Int = 300
    override val height: Int = 200
    override val geoUri = ""
    override val name: String = "Preview Location"
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
}
