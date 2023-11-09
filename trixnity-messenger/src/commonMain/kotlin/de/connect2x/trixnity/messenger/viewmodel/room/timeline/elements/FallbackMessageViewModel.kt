package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn


interface FallbackMessageViewModelFactory {
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
        fallbackMessage: String,
        referencedMessage: Flow<ReferencedMessage?>,
        message: String,
        formattedBody: String?,
        invitation: Flow<String?>,
    ): FallbackMessageViewModel {
        return FallbackMessageViewModelImpl(
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
            formattedBody,
            invitation,
        )
    }

    companion object : FallbackMessageViewModelFactory
}

interface FallbackMessageViewModel : TextBasedViewModel

open class FallbackMessageViewModelImpl(
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
    override val formattedBody: String?,
    invitation: Flow<String?>,
) : FallbackMessageViewModel, MatrixClientViewModelContext by viewModelContext {
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