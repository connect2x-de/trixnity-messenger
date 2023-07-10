package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow


interface NoticeMessageViewModelFactory {
    fun newNoticeMessageViewModel(
        viewModelContext: MatrixClientViewModelContext,
        formattedDate: String,
        showDateAbove: Boolean,
        formattedTime: String?,
        isByMe: Boolean,
        showChatBubbleEdge: Boolean,
        showBigGap: Boolean,
        showSender: StateFlow<Boolean>,
        sender: StateFlow<String>,
        fallbackMessage: String,
        referencedMessage: StateFlow<ReferencedMessage?>,
        message: String,
        invitation: Flow<String?>,
    ): NoticeMessageViewModel {
        return NoticeMessageViewModelImpl(
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

interface NoticeMessageViewModel : TextBasedViewModel

open class NoticeMessageViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    override val formattedDate: String,
    override val showDateAbove: Boolean,
    override val formattedTime: String?,
    override val isByMe: Boolean,
    override val showChatBubbleEdge: Boolean,
    override val showBigGap: Boolean,
    override val showSender: StateFlow<Boolean>,
    override val sender: StateFlow<String>,
    override val fallbackMessage: String,
    override val referencedMessage: StateFlow<ReferencedMessage?>,
    override val message: String,
    override val invitation: Flow<String?>,
) : NoticeMessageViewModel, MatrixClientViewModelContext by viewModelContext {

    override fun toString(): String {
        return fallbackMessage
    }
}