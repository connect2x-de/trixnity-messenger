package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.i18n
import kotlinx.coroutines.flow.*
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.core.model.events.m.room.CreateEventContent

interface RoomCreatedStatusViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        timelineEvent: TimelineEvent?,
        content: CreateEventContent,
        formattedDate: String,
        showDateAbove: Boolean,
        invitation: Flow<String?>,
        sender: Flow<UserInfoElement?>,
        isDirectFlow: StateFlow<Boolean>,
    ): RoomCreatedStatusViewModel {
        return RoomCreatedStatusViewModelImpl(
            viewModelContext, timelineEvent, content, formattedDate, showDateAbove, invitation, sender, isDirectFlow,
        )
    }

    companion object : RoomCreatedStatusViewModelFactory
}

interface RoomCreatedStatusViewModel : BaseTimelineElementViewModel {
    val roomCreatedMessage: StateFlow<String?>
}

open class RoomCreatedStatusViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    timelineEvent: TimelineEvent?,
    content: CreateEventContent,
    override val formattedDate: String,
    override val showDateAbove: Boolean,
    invitation: Flow<String?>,
    sender: Flow<UserInfoElement?>,
    isDirectFlow: StateFlow<Boolean>,
) : MatrixClientViewModelContext by viewModelContext, RoomCreatedStatusViewModel {
    override val invitation: StateFlow<String?> =
        invitation.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    override val roomCreatedMessage = combine(sender, isDirectFlow) { userInfo, isDirect ->
        val chatOrGroup =
            if (isDirect) i18n.eventChangeChatAccusative()
            else i18n.eventChangeGroupAccusative()
        i18n.eventRoomCreated(userInfo?.name ?: i18n.commonUnknown(), chatOrGroup)
    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
}