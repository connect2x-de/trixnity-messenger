package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import kotlinx.coroutines.flow.*


interface TextMessageViewModelFactory {
    fun newTextMessageViewModel(
        viewModelContext: MatrixClientViewModelContext,
        formattedDate: String,
        showDateAbove: Boolean,
        formattedTime: String?,
        isByMe: Boolean,
        showChatBubbleEdge: Boolean,
        showBigGap: Boolean,
        showSender: Flow<Boolean>,
        sender: Flow<String>,
        fallbackMessage: String,
        referencedMessage: Flow<ReferencedMessage?>,
        message: String,
        invitation: Flow<String?>,
    ): TextMessageViewModel {
        return TextMessageViewModelImpl(
            viewModelContext,
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
            invitation
        )
    }
}

interface TextMessageViewModel : TextBasedViewModel

open class TextMessageViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    override val formattedDate: String,
    override val showDateAbove: Boolean,
    override val formattedTime: String?,
    override val isByMe: Boolean,
    override val showChatBubbleEdge: Boolean,
    override val showBigGap: Boolean,
    showSender: Flow<Boolean>,
    sender: Flow<String>,
    override val fallbackMessage: String,
    referencedMessage: Flow<ReferencedMessage?>,
    override val message: String,
    invitation: Flow<String?>,
) : TextMessageViewModel, MatrixClientViewModelContext by viewModelContext {

    override val invitation: StateFlow<String?> =
        invitation.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
    override val sender: StateFlow<String> =
        sender.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), "")
    override val showSender: StateFlow<Boolean> =
        showSender.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), true)
    override val referencedMessage: StateFlow<ReferencedMessage?> =
        referencedMessage.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    override fun toString(): String {
        return fallbackMessage
    }
}

class PreviewTextMessageViewModel1() : TextMessageViewModel {
    override val message: String = "Hello World!"
    override val fallbackMessage: String = "Hello World!"
    override val isByMe: Boolean = false
    override val showChatBubbleEdge: Boolean = false
    override val showBigGap: Boolean = false
    override val showSender: StateFlow<Boolean> = MutableStateFlow(true)
    override val sender: StateFlow<String> = MutableStateFlow("Martin")
    override val formattedTime: String? = null
    override val invitation: StateFlow<String?> = MutableStateFlow(null)
    override val formattedDate: String = "23.12.21"
    override val showDateAbove: Boolean = true
    override val referencedMessage: MutableStateFlow<ReferencedMessage?> = MutableStateFlow(null)

}