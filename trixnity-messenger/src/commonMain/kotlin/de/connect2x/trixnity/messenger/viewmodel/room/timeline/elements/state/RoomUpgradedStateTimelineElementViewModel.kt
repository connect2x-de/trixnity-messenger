package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EventIdOrTransactionId
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel.State
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModelFactory
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.store.version
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.TombstoneEventContent
import kotlin.reflect.KClass

interface RoomUpgradedStateTimelineElementViewModelFactory : TimelineElementViewModelFactory<TombstoneEventContent> {
    override fun create(
        viewModelContext: MatrixClientViewModelContext,
        content: TombstoneEventContent,
        roomId: RoomId,
        eventIdOrTransactionId: EventIdOrTransactionId,
        onOpenMention: OpenMentionCallback,
    ): RoomUpgradedStateTimelineElementViewModel? =
        if (eventIdOrTransactionId is EventIdOrTransactionId.EventId)
            RoomUpgradedStateTimelineElementViewModelImpl(
                viewModelContext,
                content,
                roomId,
                eventIdOrTransactionId.eventId,
            ) else null


    override val supports: KClass<TombstoneEventContent>
        get() = TombstoneEventContent::class

    companion object : RoomUpgradedStateTimelineElementViewModelFactory
}

interface RoomUpgradedStateTimelineElementViewModel : State<TombstoneEventContent> {
    val changeMessage: StateFlow<String>
}

class RoomUpgradedStateTimelineElementViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    content: TombstoneEventContent,
    roomId: RoomId,
    eventId: EventId,
) : RoomUpgradedStateTimelineElementViewModel, MatrixClientViewModelContext by viewModelContext {
    override val changeMessage: StateFlow<String> = matrixClient.room.getById(content.replacementRoom).map {
        i18n.roomUpgraded(it?.version)
    }.stateIn(coroutineScope, WhileSubscribed(), i18n.roomUpgraded())

}
