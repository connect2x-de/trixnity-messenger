package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EventIdOrTransactionId
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel.State
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.TombstoneEventContent
import kotlin.reflect.KClass

interface TombstoneStateTimelineElementViewModelFactory : TimelineElementViewModelFactory<TombstoneEventContent> {
    override fun create(
        viewModelContext: MatrixClientViewModelContext,
        content: TombstoneEventContent,
        roomId: RoomId,
        eventIdOrTransactionId: EventIdOrTransactionId,
        onOpenMention: OpenMentionCallback,
    ): TombstoneStateTimelineElementViewModel? =
        if (eventIdOrTransactionId is EventIdOrTransactionId.EventId)
            TombstoneStateTimelineElementViewModelImpl(
                viewModelContext,
                content,
                roomId,
                eventIdOrTransactionId.eventId,
            ) else null


    override val supports: KClass<TombstoneEventContent>
        get() = TombstoneEventContent::class

    companion object : TombstoneStateTimelineElementViewModelFactory
}

interface TombstoneStateTimelineElementViewModel : State<TombstoneEventContent> {
    val changeMessage: StateFlow<String>
}

class TombstoneStateTimelineElementViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    content: TombstoneEventContent,
    roomId: RoomId,
    eventId: EventId,
) : TombstoneStateTimelineElementViewModel, MatrixClientViewModelContext by viewModelContext {
    override val changeMessage: StateFlow<String> = MutableStateFlow(i18n.roomUpgraded(content.body))

}
