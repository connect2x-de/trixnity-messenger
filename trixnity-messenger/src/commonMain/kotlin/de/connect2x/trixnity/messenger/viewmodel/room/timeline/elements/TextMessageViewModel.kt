package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.toUserInfoElement
import kotlinx.coroutines.flow.*
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.core.MatrixRegex
import net.folivo.trixnity.core.model.RoomId
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
            roomId
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
) : TextMessageViewModel, MatrixClientViewModelContext by viewModelContext {

    override val invitation: StateFlow<String?> =
        invitation.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
    override val sender: StateFlow<UserInfoElement> =
        sender.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), UserInfoElement(""))
    override val showSender: StateFlow<Boolean> =
        showSender.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), true)
    override val referencedMessage: StateFlow<ReferencedMessage?> =
        referencedMessage.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
    override val mentionedUsersInFormattedBody: StateFlow<Map<String, UserInfoElement>?> =
    // To the reviewer: Added !! because IntelliJ complained, however,
        // it may just not infer types properly due to its current state...
        (if (formattedBody == null) null else combine(
            MatrixRegex.findUserMentions(formattedBody!!)
        ) { allMentionedUsers ->
            allMentionedUsers.mapValues { it.toUserInfoElement(matrixClient, roomId) }
        })?.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null) ?: MutableStateFlow(null)
    override val mentionedUsersInMessage: StateFlow<Map<String, UserInfoElement>> =
        combine(MatrixRegex.findUserMentions(message)) { allMentionedUsers ->
            allMentionedUsers.mapValues { it.toUserInfoElement(matrixClient, roomId) }
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

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
    override val sender: StateFlow<UserInfoElement> = MutableStateFlow(UserInfoElement("Martin"))
    override val formattedTime: String? = null
    override val invitation: StateFlow<String?> = MutableStateFlow(null)
    override val formattedDate: String = "23.12.21"
    override val showDateAbove: Boolean = true
    override val referencedMessage: MutableStateFlow<ReferencedMessage?> = MutableStateFlow(null)
    override val mentionedUsers: Map<String, UserInfoElement> = mapOf()
}