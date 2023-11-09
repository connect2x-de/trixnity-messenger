package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import kotlinx.coroutines.flow.*


interface RedactedMessageViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        formattedDate: String,
        showDateAbove: Boolean,
        formattedTime: String?,
        isByMe: Boolean,
        showChatBubbleEdge: Boolean,
        showBigGap: Boolean,
        showSender: Flow<Boolean>,
        sender: Flow<String>,
        invitation: Flow<String?>,
    ): RedactedMessageViewModel {
        return RedactedMessageViewModelImpl(
            viewModelContext,
            formattedDate,
            showDateAbove,
            formattedTime,
            isByMe,
            showChatBubbleEdge,
            showBigGap,
            showSender,
            sender,
            invitation,
        )
    }

    companion object : RedactedMessageViewModelFactory
}

interface RedactedMessageViewModel : RoomMessageViewModel {
    val formattedMessage: StateFlow<String>
}

open class RedactedMessageViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    override val formattedDate: String,
    override val showDateAbove: Boolean,
    override val formattedTime: String?,
    override val isByMe: Boolean,
    override val showChatBubbleEdge: Boolean,
    override val showBigGap: Boolean,
    showSender: Flow<Boolean>,
    sender: Flow<String>,
    invitation: Flow<String?>,
) : RedactedMessageViewModel, MatrixClientViewModelContext by viewModelContext {
    override val invitation: StateFlow<String?> =
        invitation.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
    override val sender: StateFlow<String> =
        sender.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), "")
    override val showSender: StateFlow<Boolean> =
        showSender.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), true)

    override val formattedMessage = sender.map { username ->
        i18n.eventMessageRedacted(username)
    }.stateIn(
        coroutineScope,
        SharingStarted.WhileSubscribed(),
        i18n.eventMessageRedacted(i18n.commonUnknown())
    )
}

class PreviewRedactedMessageViewModel() : RedactedMessageViewModel {
    override val formattedMessage: StateFlow<String> = MutableStateFlow("deleted by Martin")
    override val isByMe: Boolean = false
    override val showChatBubbleEdge: Boolean = false
    override val showBigGap: Boolean = false
    override val showSender: StateFlow<Boolean> = MutableStateFlow(true)
    override val sender: StateFlow<String> = MutableStateFlow("Martin")
    override val formattedTime: String? = null
    override val invitation: StateFlow<String?> = MutableStateFlow(null)
    override val formattedDate: String = "23.12.21"
    override val showDateAbove: Boolean = false
}