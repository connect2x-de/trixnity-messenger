package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message

import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.events.m.room.RoomMessageEventContent.FileBased
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EventIdOrTransactionId
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.file.FileBasedTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.file.FileTimelineElementViewModelImpl
import kotlin.reflect.KClass

interface FileRoomMessageTimelineElementViewModelFactory : TimelineElementViewModelFactory<FileBased.File> {
    override fun create(
        viewModelContext: MatrixClientViewModelContext,
        content: FileBased.File,
        roomId: RoomId,
        eventIdOrTransactionId: EventIdOrTransactionId,
        onOpenMention: OpenMentionCallback,
    ): RoomMessageTimelineElementViewModel.FileBased.File? =
        FileRoomMessageTimelineElementViewModelImpl(viewModelContext, content, roomId, onOpenMention)

    override val supports: KClass<FileBased.File>
        get() = FileBased.File::class

    companion object : FileRoomMessageTimelineElementViewModelFactory
}

class FileRoomMessageTimelineElementViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    content: FileBased.File,
    roomId: RoomId,
    onOpenMention: OpenMentionCallback,
) :
    RoomMessageTimelineElementViewModel.FileBased.File,
    RoomMessageTimelineElementViewModelImpl<FileBased.File>(viewModelContext, content, roomId, onOpenMention),
    FileBasedTimelineElementViewModel.File by FileTimelineElementViewModelImpl(
        viewModelContext = viewModelContext,
        url = content.url,
        encryptedFile = content.file,
        info = content.info,
        fileName = content.fileName ?: content.body,
    ) {
    override val hasCaption: Boolean = content.fileName != null && content.body != content.fileName
}
