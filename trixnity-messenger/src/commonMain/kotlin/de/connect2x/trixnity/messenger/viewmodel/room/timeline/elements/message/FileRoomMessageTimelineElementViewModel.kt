package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EventIdOrTransactionId
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModelFactory
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.FileBased
import kotlin.reflect.KClass


interface FileRoomMessageTimelineElementViewModelFactory : TimelineElementViewModelFactory<FileBased.File> {
    override fun create(
        viewModelContext: MatrixClientViewModelContext,
        content: FileBased.File,
        roomId: RoomId,
        eventIdOrTransactionId: EventIdOrTransactionId,
        onOpenMention: OpenMentionCallback,
    ): RoomMessageTimelineElementViewModel.FileBased.File? =
        FileRoomMessageTimelineElementViewModelImpl(
            viewModelContext,
            content,
        )

    override val supports: KClass<FileBased.File>
        get() = FileBased.File::class

    companion object : FileRoomMessageTimelineElementViewModelFactory
}

class FileRoomMessageTimelineElementViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    content: FileBased.File,
) : RoomMessageTimelineElementViewModel.FileBased.File,
    FileBasedRoomMessageTimelineElementViewModel<FileBased.File>(viewModelContext, content)
