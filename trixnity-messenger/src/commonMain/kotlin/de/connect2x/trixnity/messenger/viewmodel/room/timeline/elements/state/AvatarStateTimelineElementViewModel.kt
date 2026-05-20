package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state

import de.connect2x.trixnity.client.room
import de.connect2x.trixnity.client.store.sender
import de.connect2x.trixnity.client.user
import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.events.m.room.AvatarEventContent
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

interface AvatarStateTimelineElementViewModelFactory : TimelineElementViewModelFactory<AvatarEventContent> {
    override fun create(
        viewModelContext: MatrixClientViewModelContext,
        content: AvatarEventContent,
        roomId: RoomId,
        eventIdOrTransactionId: EventIdOrTransactionId,
        onOpenMention: OpenMentionCallback,
    ): AvatarStateTimelineElementViewModel? =
        if (eventIdOrTransactionId is EventIdOrTransactionId.EventId)
            AvatarStateTimelineElementViewModelImpl(viewModelContext, roomId, eventIdOrTransactionId.eventId)
        else null

    override val supports: KClass<AvatarEventContent>
        get() = AvatarEventContent::class

    companion object : AvatarStateTimelineElementViewModelFactory
}

interface AvatarStateTimelineElementViewModel : State<AvatarEventContent> {
    val changeMessage: StateFlow<String?>
}

class AvatarStateTimelineElementViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    roomId: RoomId,
    eventId: EventId,
) : AvatarStateTimelineElementViewModel, MatrixClientViewModelContext by viewModelContext {
    override val changeMessage =
        flow {
                val timelineEvent = matrixClient.room.getTimelineEvent(roomId, eventId).filterNotNull().first()
                emitAll(
                    combine(
                        matrixClient.user.getById(roomId, timelineEvent.sender),
                        matrixClient.room.getById(roomId).filterNotNull().map { it.isDirect },
                    ) { userInfo, isDirect ->
                        val groupOrChat =
                            if (isDirect) i18n.eventChangeChatGenitive() else i18n.eventChangeGroupGenitive()

                        i18n.eventRoomAvatarChange(userInfo?.name ?: timelineEvent.sender.full, groupOrChat)
                    }
                )
            }
            .stateIn(coroutineScope, whileSubscribedWithTimeout, null)
}
