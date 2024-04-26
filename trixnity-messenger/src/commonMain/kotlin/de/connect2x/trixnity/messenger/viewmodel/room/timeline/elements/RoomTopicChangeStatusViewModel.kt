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
import net.folivo.trixnity.core.model.events.m.room.TopicEventContent

interface RoomTopicChangeStatusViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        timelineEvent: TimelineEvent?,
        content: TopicEventContent,
        formattedDate: String,
        showDateAbove: Boolean,
        invitation: Flow<String?>,
        sender: Flow<UserInfoElement>,
        isDirectFlow: StateFlow<Boolean>,
    ): RoomTopicChangeStatusViewModel {
        return RoomTopicChangeStatusViewModelImpl(
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

    companion object : RoomTopicChangeStatusViewModelFactory
}

interface RoomTopicChangeStatusViewModel : BaseTimelineElementViewModel {
    val roomTopicChangeMessage: StateFlow<String?>
}

open class RoomTopicChangeStatusViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    timelineEvent: TimelineEvent?,
    content: TopicEventContent,
    override val formattedDate: String,
    override val showDateAbove: Boolean,
    invitation: Flow<String?>,
    sender: Flow<UserInfoElement>,
    isDirectFlow: StateFlow<Boolean>,
) : MatrixClientViewModelContext by viewModelContext, RoomTopicChangeStatusViewModel {
    override val invitation: StateFlow<String?> =
        invitation.stateIn(coroutineScope, WhileSubscribed(), null)

    override val roomTopicChangeMessage =
        combine(sender, isDirectFlow) { userInfo, isDirect ->
            val unsigned = timelineEvent?.event?.unsigned
            val previousContent =
                if (unsigned is UnsignedRoomEventData.UnsignedStateEventData) unsigned.previousContent else null
            val from = if (previousContent is TopicEventContent) {
                i18n.eventChangeFrom(previousContent.topic)
            } else ""

            val groupOrChat =
                if (isDirect) i18n.eventChangeChatGenitive()
                else i18n.eventChangeGroupGenitive()

            i18n.eventRoomTopicChange(userInfo.name, groupOrChat, from, content.topic)
        }.stateIn(coroutineScope, WhileSubscribed(), null)
}
