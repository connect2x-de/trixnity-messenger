package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message

import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.events.m.room.RoomMessageEventContent.FileBased
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EventIdOrTransactionId
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.file.FileBasedTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.file.VideoTimelineElementViewModelImpl
import kotlin.reflect.KClass

interface VideoRoomMessageTimelineElementViewModelFactory : TimelineElementViewModelFactory<FileBased.Video> {
    override fun create(
        viewModelContext: MatrixClientViewModelContext,
        content: FileBased.Video,
        roomId: RoomId,
        eventIdOrTransactionId: EventIdOrTransactionId,
        onOpenMention: OpenMentionCallback,
    ): RoomMessageTimelineElementViewModel.FileBased.Video? =
        VideoRoomMessageTimelineElementViewModelImpl(viewModelContext, content, roomId, onOpenMention)

    override val supports: KClass<FileBased.Video>
        get() = FileBased.Video::class

    companion object : VideoRoomMessageTimelineElementViewModelFactory
}

class VideoRoomMessageTimelineElementViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    content: FileBased.Video,
    roomId: RoomId,
    onOpenMention: OpenMentionCallback,
) :
    RoomMessageTimelineElementViewModel.FileBased.Video,
    RoomMessageTimelineElementViewModelImpl<FileBased.Video>(viewModelContext, content, roomId, onOpenMention),
    FileBasedTimelineElementViewModel.Video by VideoTimelineElementViewModelImpl(
        viewModelContext = viewModelContext,
        url = content.url,
        encryptedFile = content.file,
        info = content.info,
        fileName = content.fileName ?: content.body,
    ) {
    override val hasCaption: Boolean = content.fileName != null && content.body != content.fileName
}
