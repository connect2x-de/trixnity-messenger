package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.i18n
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.core.model.events.UnsignedRoomEventData
import net.folivo.trixnity.core.model.events.m.room.NameEventContent

interface RoomNameChangeStatusViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        timelineEvent: TimelineEvent?,
        content: NameEventContent,
        formattedDate: String,
        showDateAbove: Boolean,
        invitation: Flow<String?>,
        sender: Flow<UserInfoElement>,
        isDirectFlow: StateFlow<Boolean>,
    ): RoomNameChangeStatusViewModel {
        return RoomNameChangeStatusViewModelImpl(
            viewModelContext,
            timelineEvent,
            content,
            formattedDate,
            showDateAbove,
            invitation,
            sender,
            isDirectFlow,
        )
    }

    companion object : RoomNameChangeStatusViewModelFactory
}

interface RoomNameChangeStatusViewModel : BaseTimelineElementViewModel {
    val roomNameChangeMessage: StateFlow<String?>
}

open class RoomNameChangeStatusViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    timelineEvent: TimelineEvent?,
    content: NameEventContent,
    override val formattedDate: String,
    override val showDateAbove: Boolean,
    invitation: Flow<String?>,
    sender: Flow<UserInfoElement>,
    isDirectFlow: StateFlow<Boolean>,
) : MatrixClientViewModelContext by viewModelContext, RoomNameChangeStatusViewModel {
    override val invitation: StateFlow<String?> =
        invitation.stateIn(coroutineScope, WhileSubscribed(), null)

    override val roomNameChangeMessage =
        combine(sender, isDirectFlow) { userInfo, isDirect ->
            val unsigned = timelineEvent?.event?.unsigned
            val previousContent =
                if (unsigned is UnsignedRoomEventData.UnsignedStateEventData) unsigned.previousContent else null
            val from = if (previousContent is NameEventContent) {
                i18n.eventRoomChangeFrom(previousContent.name)
            } else ""

            val groupOrChat =
                if (isDirect) i18n.eventChangeChatGenitive()
                else i18n.eventChangeGroupGenitive()

            i18n.eventRoomNameChange(userInfo.name, groupOrChat, from, content.name)
        }.stateIn(coroutineScope, WhileSubscribed(), null)
}
