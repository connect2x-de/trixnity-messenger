package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EventIdOrTransactionId
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel.State
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModelFactory
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
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
import net.folivo.trixnity.core.model.events.UnsignedRoomEventData
import net.folivo.trixnity.core.model.events.m.room.HistoryVisibilityEventContent
import kotlin.reflect.KClass

interface HistoryVisibilityStateTimelineElementViewModelFactory :
    TimelineElementViewModelFactory<HistoryVisibilityEventContent> {
    override fun create(
        viewModelContext: MatrixClientViewModelContext,
        content: HistoryVisibilityEventContent,
        roomId: RoomId,
        eventIdOrTransactionId: EventIdOrTransactionId,
        onOpenMention: OpenMentionCallback,
    ): HistoryVisibilityStateTimelineElementViewModel? =
        if (eventIdOrTransactionId is EventIdOrTransactionId.EventId)
            HistoryVisibilityStateTimelineElementViewModelImpl(
                viewModelContext,
                content,
                roomId,
                eventIdOrTransactionId.eventId,
            ) else null

    override val supports: KClass<HistoryVisibilityEventContent>
        get() = HistoryVisibilityEventContent::class

    companion object : HistoryVisibilityStateTimelineElementViewModelFactory
}

interface HistoryVisibilityStateTimelineElementViewModel :
    State<HistoryVisibilityEventContent> {
    val changeMessage: StateFlow<String?>
}

class HistoryVisibilityStateTimelineElementViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    content: HistoryVisibilityEventContent,
    roomId: RoomId,
    eventId: EventId,
) : HistoryVisibilityStateTimelineElementViewModel, MatrixClientViewModelContext by viewModelContext {
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
                        if (unsigned is UnsignedRoomEventData.UnsignedStateEventData) unsigned.previousContent else null
                    val from = if (previousContent is HistoryVisibilityEventContent) {
                        i18n.eventChangeFrom(translateVisibility(previousContent.historyVisibility))
                    } else ""

                    val groupOrChat =
                        if (isDirect) i18n.eventChangeChatGenitive()
                        else i18n.eventChangeGroupGenitive()

                    i18n.historyVisibilityChange(
                        userInfo?.name ?: timelineEvent.sender.full,
                        groupOrChat,
                        from,
                        translateVisibility(content.historyVisibility)
                    )
                }
            )
        }.stateIn(coroutineScope, WhileSubscribed(), null)

    private fun translateVisibility(historyVisibility: HistoryVisibilityEventContent.HistoryVisibility): String {
        return when (historyVisibility) {
            HistoryVisibilityEventContent.HistoryVisibility.SHARED -> i18n.historyVisibilityShared()
            HistoryVisibilityEventContent.HistoryVisibility.JOINED -> i18n.historyVisibilityJoined()
            HistoryVisibilityEventContent.HistoryVisibility.INVITED -> i18n.historyVisibiltyInvite()
            HistoryVisibilityEventContent.HistoryVisibility.WORLD_READABLE -> i18n.historyVisibilityWorldReadable()
        }
    }
}
