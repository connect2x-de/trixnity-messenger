package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.OpenModalType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.core.model.events.m.room.EncryptedFile
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
        onOpenModal: (type: OpenModalType, mxcUrl: String, encryptedFile: EncryptedFile?, fileName: String) -> Unit,
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
        )
    }

    companion object : AudioMessageViewModelFactory
}

interface AudioMessageViewModel : FileBasedMessageViewModel {
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
    private val onOpenModal: (type: OpenModalType, mxcUrl: String, encryptedFile: EncryptedFile?, fileName: String) -> Unit,
) : AudioMessageViewModel, AbstractFileBasedMessageViewModel(viewModelContext, content),
    MatrixClientViewModelContext by viewModelContext {
    override val invitation: StateFlow<String?> =
        invitation.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
    override val sender: StateFlow<UserInfoElement> =
        sender.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), UserInfoElement(""))
    override val showSender: StateFlow<Boolean> =
        showSender.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), true)

    override fun openAudio() {
        // TODO if you have audio working, replace with: 'url?.let { onOpenModal(OpenModalType.AUDIO, it, encryptedFile) }'
        openSaveFileDialog()
    }
}