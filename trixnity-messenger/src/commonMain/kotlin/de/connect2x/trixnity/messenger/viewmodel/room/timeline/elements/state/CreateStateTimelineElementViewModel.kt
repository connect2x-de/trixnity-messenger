package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EventIdOrTransactionId
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel.State
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.whileSubscribedWithTimeout
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.store.sender
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.CreateEventContent
import kotlin.reflect.KClass

interface CreateStateTimelineElementViewModelFactory : TimelineElementViewModelFactory<CreateEventContent> {
    override fun create(
        viewModelContext: MatrixClientViewModelContext,
        content: CreateEventContent,
        roomId: RoomId,
        eventIdOrTransactionId: EventIdOrTransactionId,
        onOpenMention: OpenMentionCallback,
    ): CreateStateTimelineElementViewModel? =
        if (eventIdOrTransactionId is EventIdOrTransactionId.EventId)
            CreateStateTimelineElementViewModelImpl(
                viewModelContext,
                roomId,
                eventIdOrTransactionId.eventId,
            ) else null

    override val supports: KClass<CreateEventContent>
        get() = CreateEventContent::class

    companion object : CreateStateTimelineElementViewModelFactory
}

interface CreateStateTimelineElementViewModel : State<CreateEventContent> {
    val message: StateFlow<String?>
}

class CreateStateTimelineElementViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    roomId: RoomId,
    eventId: EventId,
) : CreateStateTimelineElementViewModel, MatrixClientViewModelContext by viewModelContext {
    override val message =
        flow {
            val timelineEvent = matrixClient.room.getTimelineEvent(roomId, eventId).filterNotNull().first()
            emitAll(
                combine(
                    matrixClient.user.getById(roomId, timelineEvent.sender),
                    matrixClient.room.getById(roomId).filterNotNull().map { it.isDirect },
                ) { userInfo, isDirect ->
                    val chatOrGroup =
                        if (isDirect) i18n.eventChangeChatAccusative()
                        else i18n.eventChangeGroupAccusative()
                    i18n.eventRoomCreated(userInfo?.name ?: timelineEvent.sender.full, chatOrGroup)
                }
            )
        }.stateIn(coroutineScope, whileSubscribedWithTimeout, null)
}
