package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message

import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.events.m.room.RoomMessageEventContent.FileBased
import de.connect2x.trixnity.messenger.util.getOrNull
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.media.MediaPlayerViewModel
import de.connect2x.trixnity.messenger.viewmodel.media.MediaPlayerViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EventIdOrTransactionId
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModelFactory
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

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
    FileBasedRoomMessageTimelineElementViewModel<FileBased.Audio>(
        viewModelContext,
        content,
        roomId,
        eventIdOrTransactionId,
        onOpenMention,
    ) {
    override val duration: Duration? = content.info?.duration?.milliseconds
    override val audioPlayer: MediaPlayerViewModel? =
        getOrNull<MediaPlayerViewModelFactory>()
            ?.create(
                id = eventIdOrTransactionId.toString(),
                viewModelContext = viewModelContext,
                mimeType = mimeType ?: "audio/raw",
                initialDuration = duration,
                acquireFile = ::downloadMediaInternal,
            )
}
