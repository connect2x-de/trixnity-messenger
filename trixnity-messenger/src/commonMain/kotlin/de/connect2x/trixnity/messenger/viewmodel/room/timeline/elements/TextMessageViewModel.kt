package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.Mention
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.mentionsStateFlow
import kotlinx.coroutines.flow.*
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent


interface TextMessageViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        timelineEvent: TimelineEvent?,
        content: RoomMessageEventContent.TextBased,
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
        onOpenMention: (userId: UserId, mention: Mention) -> Unit
    ): TextMessageViewModel {
        return TextMessageViewModelImpl(
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
            onOpenMention
        )
    }

    companion object : TextMessageViewModelFactory
}

interface TextMessageViewModel : TextBasedViewModel

open class TextMessageViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    timelineEvent: TimelineEvent?,
    content: RoomMessageEventContent.TextBased,
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
    override val onOpenMention: (userId: UserId, mention: Mention) -> Unit
) : TextMessageViewModel, MatrixClientViewModelContext by viewModelContext {

    override val invitation: StateFlow<String?> =
        invitation.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
    override val sender: StateFlow<UserInfoElement> =
        sender.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), UserInfoElement("", UserId("")))
    override val showSender: StateFlow<Boolean> =
        showSender.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), true)
    override val referencedMessage: StateFlow<ReferencedMessage?> =
        referencedMessage.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
    override val mentionsInMessage: Map<String, StateFlow<Mention>> =
        mentionsStateFlow(message, roomId, matrixClient, coroutineScope)
    override val mentionsInFormattedBody: Map<String, StateFlow<Mention>>? =
        formattedBody?.let {
            mentionsStateFlow(it, roomId, matrixClient, coroutineScope)
        }

    override fun openMention(mention: Mention) {
        onOpenMention(matrixClient.userId, mention)
    }

    override fun toString(): String {
        return fallbackMessage
    }
}

class PreviewTextMessageViewModel1() : TextMessageViewModel {
    override val message: String = "Hello World!"
    override val formattedBody: String = "Hello <b>World!</b>"
    override val fallbackMessage: String = "Hello World!"
    override val isByMe: Boolean = false
    override val showChatBubbleEdge: Boolean = false
    override val showBigGap: Boolean = false
    override val showSender: StateFlow<Boolean> = MutableStateFlow(true)
    override val sender: StateFlow<UserInfoElement> =
        MutableStateFlow(UserInfoElement("Martin", UserId("martin:matrix.org")))
    override val formattedTime: String? = null
    override val invitation: StateFlow<String?> = MutableStateFlow(null)
    override val formattedDate: String = "23.12.21"
    override val showDateAbove: Boolean = true
    override val referencedMessage: MutableStateFlow<ReferencedMessage?> = MutableStateFlow(null)
    override val mentionsInMessage: Map<String, StateFlow<Mention>> = mapOf()
    override val mentionsInFormattedBody: Map<String, StateFlow<Mention>>? = mapOf()
    override val onOpenMention: (userId: UserId, mention: Mention) -> Unit = { _, _ -> }
    override fun openMention(mention: Mention) {}
}
