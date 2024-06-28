package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.messenger.util.FileTransferProgressElement
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.OpenModalCallback
import de.connect2x.trixnity.messenger.viewmodel.util.formatProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.clientserverapi.model.media.FileTransferProgress
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent


interface AudioMessageViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        timelineEvent: TimelineEvent?,
        content: RoomMessageEventContent.FileBased.Audio,
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
    ): AudioMessageViewModel {
        return AudioMessageViewModelImpl(
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

    companion object : AudioMessageViewModelFactory
}

interface AudioMessageViewModel : FileBasedMessageViewModel {
    val duration: Int?
    val progress: StateFlow<FileTransferProgressElement?>
    fun openAudio()
}

open class AudioMessageViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    timelineEvent: TimelineEvent?,
    content: RoomMessageEventContent.FileBased.Audio,
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
) : AudioMessageViewModel, AbstractFileBasedMessageViewModel(viewModelContext, content),
    MatrixClientViewModelContext by viewModelContext {
    override val duration: Int? = content.info?.duration
    override val invitation: StateFlow<String?> =
        invitation.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
    override val sender: StateFlow<UserInfoElement> =
        sender.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), UserInfoElement("", UserId("")))
    override val showSender: StateFlow<Boolean> =
        showSender.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), true)
    override val progress: StateFlow<FileTransferProgressElement?> = mediaUploadProgress.map {
        if (it != null) {
            FileTransferProgressElement(
                percent = if (it.total > 0) it.transferred/it.total.toFloat() else 0.0f,
                formattedProgress = formatProgress(it)
            )
        }
        else null
    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    override fun openAudio() {
        // TODO if you have audio working, replace with: 'url?.let { onOpenModal(OpenModalType.AUDIO, it, encryptedFile) }'
        openSaveFileDialog()
    }
}
