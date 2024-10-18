package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.MessageMention
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.mentionsStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent

interface FallbackMessageViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        timelineEvent: TimelineEvent?,
        content: RoomMessageEventContent.Unknown,
        formattedDate: String,
        showDateAbove: Boolean,
        formattedTime: String?,
        isByMe: Boolean,
        showChatBubbleEdge: Boolean,
        showBigGap: Boolean,
        showSender: Flow<Boolean>,
        sender: Flow<UserInfoElement>,
        fallbackMessage: String,
        referencedMessage: Flow<ReferencedMessage?>,
        message: String,
        formattedBody: String?,
        invitation: Flow<String?>,
        roomId: RoomId,
        onOpenMention: OpenMentionCallback,
    ): FallbackMessageViewModel {
        return FallbackMessageViewModelImpl(
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
            fallbackMessage,
            referencedMessage,
            message,
            formattedBody,
            invitation,
            roomId,
            onOpenMention,
        )
    }

    companion object : FallbackMessageViewModelFactory
}

interface FallbackMessageViewModel : TextBasedViewModel

open class FallbackMessageViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    timelineEvent: TimelineEvent?,
    content: RoomMessageEventContent.Unknown,
    override val formattedDate: String,
    override val showDateAbove: Boolean,
    override val formattedTime: String?,
    override val isByMe: Boolean,
    override val showChatBubbleEdge: Boolean,
    override val showBigGap: Boolean,
    showSender: Flow<Boolean>,
    sender: Flow<UserInfoElement>,
    override val fallbackMessage: String,
    referencedMessage: Flow<ReferencedMessage?>,
    override val message: String,
    override val formattedBody: String?,
    invitation: Flow<String?>,
    roomId: RoomId,
    private val onOpenMention: OpenMentionCallback
) : FallbackMessageViewModel, MatrixClientViewModelContext by viewModelContext {
    override val invitation: StateFlow<String?> =
        invitation.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
    override val sender: StateFlow<UserInfoElement> =
        sender.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), UserInfoElement("", UserId("")))
    override val showSender: StateFlow<Boolean> =
        showSender.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), true)
    override val referencedMessage: StateFlow<ReferencedMessage?> =
        referencedMessage.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
    override val mentionsInMessage: Map<IntRange, StateFlow<MessageMention?>> =
        mentionsStateFlow(message, roomId, matrixClient, coroutineScope)
    override val mentionsInFormattedBody: Map<IntRange, StateFlow<MessageMention?>>? =
        formattedBody?.let {
            mentionsStateFlow(it, roomId, matrixClient, coroutineScope)
        }

    override fun openMention(messageMention: MessageMention) {
        onOpenMention(matrixClient.userId, messageMention)
    }

    override fun toString(): String {
        return fallbackMessage
    }
}
