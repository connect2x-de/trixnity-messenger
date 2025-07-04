package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EventIdOrTransactionId
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModelFactory
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.Location
import kotlin.reflect.KClass

interface LocationRoomMessageTimelineElementViewModelFactory : TimelineElementViewModelFactory<Location> {
    override fun create(
        viewModelContext: MatrixClientViewModelContext,
        content: Location,
        roomId: RoomId,
        eventIdOrTransactionId: EventIdOrTransactionId,
        onOpenMention: OpenMentionCallback,
    ): RoomMessageTimelineElementViewModel.Location? =
        LocationRoomMessageTimelineElementViewModelImpl(
            viewModelContext,
            content,
            roomId,
            onOpenMention
        )

    override val supports: KClass<Location>
        get() = Location::class

    companion object : LocationRoomMessageTimelineElementViewModelFactory
}

class LocationRoomMessageTimelineElementViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    content: Location,
    roomId: RoomId,
    onOpenMention: OpenMentionCallback,
) : RoomMessageTimelineElementViewModel.Location,
    RoomMessageTimelineElementViewModelImpl<Location>(viewModelContext, content, roomId, onOpenMention) {
    override val name: String = content.body
    override val geoUri: String = content.geoUri
}
