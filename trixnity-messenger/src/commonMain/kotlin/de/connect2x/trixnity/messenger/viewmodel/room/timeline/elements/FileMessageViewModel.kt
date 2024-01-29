package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.util.formatSize
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent

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
    ): FileMessageViewModel {
        return FileMessageViewModelImpl(
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
            invitation
        )
    }

    companion object : FileMessageViewModelFactory
}

interface FileMessageViewModel : FileBasedMessageViewModel {
    val formattedSize: String
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
) : FileMessageViewModel, AbstractFileBasedMessageViewModel(viewModelContext, content),
    MatrixClientViewModelContext by viewModelContext {
    override val invitation: StateFlow<String?> =
        invitation.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
    override val sender: StateFlow<UserInfoElement> =
        sender.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), UserInfoElement(""))
    override val showSender: StateFlow<Boolean> =
        showSender.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), true)

    override val formattedSize: String = content.info?.size?.let { " (${formatSize(it.toLong())})" } ?: ""
}