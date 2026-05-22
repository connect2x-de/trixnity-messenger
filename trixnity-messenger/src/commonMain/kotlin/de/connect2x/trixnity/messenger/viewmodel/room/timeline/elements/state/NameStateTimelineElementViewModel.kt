package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state

import de.connect2x.trixnity.client.room
import de.connect2x.trixnity.client.store.sender
import de.connect2x.trixnity.client.user
import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.events.UnsignedRoomEventData
import de.connect2x.trixnity.core.model.events.m.room.NameEventContent
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EventIdOrTransactionId
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel.State
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.whileSubscribedWithTimeout
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

interface NameStateTimelineElementViewModelFactory : TimelineElementViewModelFactory<NameEventContent> {
    override fun create(
        viewModelContext: MatrixClientViewModelContext,
        content: NameEventContent,
        roomId: RoomId,
        eventIdOrTransactionId: EventIdOrTransactionId,
        onOpenMention: OpenMentionCallback,
    ): NameStateTimelineElementViewModel? =
        if (eventIdOrTransactionId is EventIdOrTransactionId.EventId)
            NameStateTimelineElementViewModelImpl(viewModelContext, content, roomId, eventIdOrTransactionId.eventId)
        else null

    override val supports: KClass<NameEventContent>
        get() = NameEventContent::class

    companion object : NameStateTimelineElementViewModelFactory
}

interface NameStateTimelineElementViewModel : State<NameEventContent> {
    val changeMessage: StateFlow<String?>
}

class NameStateTimelineElementViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    content: NameEventContent,
    roomId: RoomId,
    eventId: EventId,
) : NameStateTimelineElementViewModel, MatrixClientViewModelContext by viewModelContext {
    override val changeMessage =
        flow {
                val timelineEvent = matrixClient.room.getTimelineEvent(roomId, eventId).filterNotNull().first()
                emitAll(
                    combine(
                        matrixClient.user.getById(roomId, timelineEvent.sender),
                        matrixClient.room.getById(roomId).filterNotNull().map { it.isDirect },
                    ) { userInfo, isDirect ->
                        val unsigned = timelineEvent.event.unsigned
                        val previousContent =
                            if (unsigned is UnsignedRoomEventData.UnsignedStateEventData) unsigned.previousContent
                            else null
                        val from =
                            if (previousContent is NameEventContent) {
                                i18n.eventChangeFrom(previousContent.name)
                            } else ""

                        val groupOrChat =
                            if (isDirect) i18n.eventChangeChatGenitive() else i18n.eventChangeGroupGenitive()

                        i18n.eventRoomNameChange(
                            userInfo?.name ?: timelineEvent.sender.full,
                            groupOrChat,
                            from,
                            content.name,
                        )
                    }
                )
            }
            .stateIn(coroutineScope, whileSubscribedWithTimeout, null)
}
