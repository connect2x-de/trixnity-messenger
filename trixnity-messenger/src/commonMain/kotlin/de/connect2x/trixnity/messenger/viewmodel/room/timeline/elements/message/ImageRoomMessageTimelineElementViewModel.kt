package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message

import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.events.m.room.RoomMessageEventContent.FileBased
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EventIdOrTransactionId
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.file.FileBasedTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.file.ImageTimelineElementViewModelImpl
import kotlin.reflect.KClass

interface ImageRoomMessageTimelineElementViewModelFactory : TimelineElementViewModelFactory<FileBased.Image> {
    override fun create(
        viewModelContext: MatrixClientViewModelContext,
        content: FileBased.Image,
        roomId: RoomId,
        eventIdOrTransactionId: EventIdOrTransactionId,
        onOpenMention: OpenMentionCallback,
    ): RoomMessageTimelineElementViewModel.FileBased.Image? =
        ImageRoomMessageTimelineElementViewModelImpl(viewModelContext, content, roomId, onOpenMention)

    override val supports: KClass<FileBased.Image>
        get() = FileBased.Image::class

    companion object : ImageRoomMessageTimelineElementViewModelFactory
}

class ImageRoomMessageTimelineElementViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    content: FileBased.Image,
    roomId: RoomId,
    onOpenMention: OpenMentionCallback,
) :
    RoomMessageTimelineElementViewModel.FileBased.Image,
    RoomMessageTimelineElementViewModelImpl<FileBased.Image>(viewModelContext, content, roomId, onOpenMention),
    FileBasedTimelineElementViewModel.Image by ImageTimelineElementViewModelImpl(
        viewModelContext = viewModelContext,
        url = content.url,
        encryptedFile = content.file,
        info = content.info,
        fileName = content.fileName ?: content.body,
    ) {
    override val hasCaption: Boolean = content.fileName != null && content.body != content.fileName
}
