package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message

import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.events.m.room.RoomMessageEventContent.FileBased
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EventIdOrTransactionId
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.file.AudioTimelineElementViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.file.FileBasedTimelineElementViewModel
import kotlin.reflect.KClass

interface AudioRoomMessageTimelineElementViewModelFactory : TimelineElementViewModelFactory<FileBased.Audio> {
    override fun create(
        viewModelContext: MatrixClientViewModelContext,
        content: FileBased.Audio,
        roomId: RoomId,
        eventIdOrTransactionId: EventIdOrTransactionId,
        onOpenMention: OpenMentionCallback,
    ): RoomMessageTimelineElementViewModel.FileBased.Audio? =
        AudioRoomMessageTimelineElementViewModelImpl(
            viewModelContext,
            content,
            roomId,
            eventIdOrTransactionId,
            onOpenMention,
        )

    override val supports: KClass<FileBased.Audio>
        get() = FileBased.Audio::class

    companion object : AudioRoomMessageTimelineElementViewModelFactory
}

class AudioRoomMessageTimelineElementViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    content: FileBased.Audio,
    roomId: RoomId,
    eventIdOrTransactionId: EventIdOrTransactionId,
    onOpenMention: OpenMentionCallback,
) :
    RoomMessageTimelineElementViewModel.FileBased.Audio,
    RoomMessageTimelineElementViewModelImpl<FileBased.Audio>(viewModelContext, content, roomId, onOpenMention),
    FileBasedTimelineElementViewModel.Audio by AudioTimelineElementViewModelImpl(
        viewModelContext = viewModelContext,
        url = content.url,
        encryptedFile = content.file,
        info = content.info,
        fileName = content.fileName ?: content.body,
        roomId = roomId,
        eventIdOrTransactionId = eventIdOrTransactionId,
    ) {
    override val hasCaption: Boolean = content.fileName != null && content.body != content.fileName
}
