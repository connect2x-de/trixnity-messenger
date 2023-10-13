package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import kotlinx.coroutines.flow.*

interface RoomCreatedStatusViewModelFactory {
    fun newRoomCreatedStatusViewModel(
        viewModelContext: MatrixClientViewModelContext,
        formattedDate: String,
        showDateAbove: Boolean,
        invitation: Flow<String?>,
        sender: Flow<String?>,
        isDirectFlow: StateFlow<Boolean>,
    ): RoomCreatedStatusViewModel {
        return RoomCreatedStatusViewModelImpl(
            viewModelContext, formattedDate, showDateAbove, invitation, sender, isDirectFlow,
        )
    }
}

interface RoomCreatedStatusViewModel : BaseTimelineElementViewModel {
    val roomCreatedMessage: StateFlow<String?>
}

open class RoomCreatedStatusViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    override val formattedDate: String,
    override val showDateAbove: Boolean,
    invitation: Flow<String?>,
    sender: Flow<String?>,
    isDirectFlow: StateFlow<Boolean>,
) : MatrixClientViewModelContext by viewModelContext, RoomCreatedStatusViewModel {
    override val invitation: StateFlow<String?> =
        invitation.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    override val roomCreatedMessage = combine(sender, isDirectFlow) { username, isDirect ->
        val chatOrGroup =
            if (isDirect) i18n.eventChangeChatAccusative()
            else i18n.eventChangeGroupAccusative()
        i18n.eventRoomCreated(username ?: i18n.commonUnknown(), chatOrGroup)
    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
}