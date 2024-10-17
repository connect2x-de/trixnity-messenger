package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.messenger.util.FileTransferProgressElement
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.files.MediaConstants.MAX_SIZE_DOCUMENT_PREVIEW_BYTES
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.OpenModalCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.OpenModalType
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.Thumbnails
import de.connect2x.trixnity.messenger.viewmodel.util.formatSize
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.clientserverapi.model.media.FileTransferProgress
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import org.koin.core.component.get


interface FileMessageViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        timelineEvent: TimelineEvent?,
        content: RoomMessageEventContent.FileBased.File,
        formattedDate: String,
        showDateAbove: Boolean,
        formattedTime: String?,
        isByMe: Boolean,
        showChatBubbleEdge: Boolean,
        showBigGap: Boolean,
        showSender: Flow<Boolean>,
        sender: Flow<UserInfoElement>,
        invitation: Flow<String?>,
        mediaUploadProgress: MutableStateFlow<FileTransferProgress?>,
        onOpenModal: OpenModalCallback,
    ): FileMessageViewModel = FileMessageViewModelImpl(
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
        mediaUploadProgress,
        onOpenModal,
    )

    companion object : FileMessageViewModelFactory
}

interface FileMessageViewModel : FileBasedMessageViewModel {
    val formattedSize: String
    fun openFile()
}

open class FileMessageViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    timelineEvent: TimelineEvent?,
    content: RoomMessageEventContent.FileBased.File,
    override val formattedDate: String,
    override val showDateAbove: Boolean,
    override val formattedTime: String?,
    override val isByMe: Boolean,
    override val showChatBubbleEdge: Boolean,
    override val showBigGap: Boolean,
    showSender: Flow<Boolean>,
    sender: Flow<UserInfoElement>,
    invitation: Flow<String?>,
    mediaUploadProgress: MutableStateFlow<FileTransferProgress?>,
    private val onOpenModal: OpenModalCallback,
) : FileMessageViewModel, AbstractFileBasedMessageViewModel(viewModelContext, content, onOpenModal),
    MatrixClientViewModelContext by viewModelContext {
    override val showReactions: StateFlow<Boolean> = MutableStateFlow(true)
    override val invitation: StateFlow<String?> =
        invitation.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
    override val sender: StateFlow<UserInfoElement> =
        sender.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), UserInfoElement("", UserId("")))
    override val showSender: StateFlow<Boolean> =
        showSender.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), true)

    override val formattedSize: String = content.info?.size?.let { " (${formatSize(it.toLong())})" } ?: ""
    private val thumbnails = get<Thumbnails>()
    override val uploadProgress: StateFlow<FileTransferProgressElement?> =
        thumbnails.mapProgressToProgressElement(mediaUploadProgress)
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    override fun openFile() {
        if ((fileSize ?: 0) > MAX_SIZE_DOCUMENT_PREVIEW_BYTES) {
            openSaveFileDialog()
        } else when (fileMimeType) {
            "application/pdf" -> url?.let { onOpenModal(OpenModalType.PDF, it, encryptedFile, fileName) }
            "text/markdown" -> url?.let { onOpenModal(OpenModalType.MARKDOWN, it, encryptedFile, fileName) }
            "text/plain" -> url?.let { onOpenModal(OpenModalType.TEXT, it, encryptedFile, fileName) }
            else -> openSaveFileDialog()
        }
    }
}
