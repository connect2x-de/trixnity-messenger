package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EventIdOrTransactionId
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModelFactory
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.TextBased
import kotlin.reflect.KClass

interface NoticeRoomMessageTimelineElementViewModelFactory : TimelineElementViewModelFactory<TextBased.Notice> {
    override fun create(
        viewModelContext: MatrixClientViewModelContext,
        content: TextBased.Notice,
        roomId: RoomId,
        eventIdOrTransactionId: EventIdOrTransactionId,
        onOpenMention: OpenMentionCallback,
    ): RoomMessageTimelineElementViewModel.TextBased.Notice? =
        NoticeRoomMessageTimelineElementViewModelImpl(
            viewModelContext,
            content,
            roomId,
            onOpenMention
        )

    override val supports: KClass<TextBased.Notice>
        get() = TextBased.Notice::class

    companion object : NoticeRoomMessageTimelineElementViewModelFactory
}

class NoticeRoomMessageTimelineElementViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    content: TextBased.Notice,
    roomId: RoomId,
    onOpenMention: OpenMentionCallback,
) : RoomMessageTimelineElementViewModel.TextBased.Notice,
    TextBasedRoomMessageTimelineElementViewModel<TextBased.Notice>(viewModelContext, content, roomId, onOpenMention)
