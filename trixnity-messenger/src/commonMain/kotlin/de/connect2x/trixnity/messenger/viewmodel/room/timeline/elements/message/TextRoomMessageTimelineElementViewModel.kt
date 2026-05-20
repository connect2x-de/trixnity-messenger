package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message

import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.events.m.room.RoomMessageEventContent.TextBased
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EventIdOrTransactionId
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModelFactory
import kotlin.reflect.KClass

interface TextRoomMessageTimelineElementViewModelFactory : TimelineElementViewModelFactory<TextBased.Text> {
    override fun create(
        viewModelContext: MatrixClientViewModelContext,
        content: TextBased.Text,
        roomId: RoomId,
        eventIdOrTransactionId: EventIdOrTransactionId,
        onOpenMention: OpenMentionCallback,
    ): RoomMessageTimelineElementViewModel.TextBased.Text? =
        TextRoomMessageTimelineElementViewModelImpl(viewModelContext, content, roomId, onOpenMention)

    override val supports: KClass<TextBased.Text>
        get() = TextBased.Text::class

    companion object : TextRoomMessageTimelineElementViewModelFactory
}

class TextRoomMessageTimelineElementViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    content: TextBased.Text,
    roomId: RoomId,
    onOpenMention: OpenMentionCallback,
) :
    RoomMessageTimelineElementViewModel.TextBased.Text,
    RoomMessageTimelineElementViewModelImpl<TextBased.Text>(viewModelContext, content, roomId, onOpenMention)
