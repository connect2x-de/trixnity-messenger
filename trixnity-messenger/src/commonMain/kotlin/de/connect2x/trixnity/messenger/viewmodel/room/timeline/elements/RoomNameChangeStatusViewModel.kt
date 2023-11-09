package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import kotlinx.coroutines.flow.*
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.core.model.events.UnsignedRoomEventData
import net.folivo.trixnity.core.model.events.m.room.NameEventContent

interface RoomNameChangeStatusViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        formattedDate: String,
        showDateAbove: Boolean,
        invitation: Flow<String?>,
        sender: Flow<String>,
        timelineEvent: TimelineEvent,
        isDirectFlow: StateFlow<Boolean>,
    ): RoomNameChangeStatusViewModel {
        return RoomNameChangeStatusViewModelImpl(
            viewModelContext,
            formattedDate,
            showDateAbove,
            invitation,
            sender,
            timelineEvent,
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
    override val formattedDate: String,
    override val showDateAbove: Boolean,
    invitation: Flow<String?>,
    sender: Flow<String>,
    timelineEvent: TimelineEvent,
    isDirectFlow: StateFlow<Boolean>,
) : MatrixClientViewModelContext by viewModelContext, RoomNameChangeStatusViewModel {
    override val invitation: StateFlow<String?> =
        invitation.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    override val roomNameChangeMessage =
        combine(sender, isDirectFlow) { username, isDirect ->
            val content = timelineEvent.event.content
            require(content is NameEventContent)

            val unsigned = timelineEvent.event.unsigned
            val previousContent =
                if (unsigned is UnsignedRoomEventData.UnsignedStateEventData<*>) unsigned.previousContent else null
            val from = if (previousContent is NameEventContent) {
                i18n.eventRoomChangeFrom(previousContent.name)
            } else ""

            val groupOrChat =
                if (isDirect) i18n.eventChangeChatGenitive()
                else i18n.eventChangeGroupGenitive()

            i18n.eventRoomChange(username, groupOrChat, from, content.name)
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
}