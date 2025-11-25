package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message

import de.connect2x.trixnity.messenger.util.getOrNull
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.media.AudioPlayerViewModel
import de.connect2x.trixnity.messenger.viewmodel.media.AudioPlayerViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EventIdOrTransactionId
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModelFactory
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.FileBased
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
) : RoomMessageTimelineElementViewModel.FileBased.Audio, FileBasedRoomMessageTimelineElementViewModel<FileBased.Audio>(
    viewModelContext,
    content,
    roomId,
    eventIdOrTransactionId,
    onOpenMention
) {
    override val duration: Long? = content.info?.duration
    override val audioPlayer: AudioPlayerViewModel? = getOrNull<AudioPlayerViewModelFactory>()
        ?.create(viewModelContext, this)
}
