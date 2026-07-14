package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message

import de.connect2x.trixnity.clientserverapi.model.media.FileTransferProgress
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.events.m.room.RoomMessageEventContent.FileBased
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EventIdOrTransactionId
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.Thumbnails
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.whileSubscribedWithTimeout
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.koin.core.component.get

interface VideoRoomMessageTimelineElementViewModelFactory : TimelineElementViewModelFactory<FileBased.Video> {
    override fun create(
        viewModelContext: MatrixClientViewModelContext,
        content: FileBased.Video,
        roomId: RoomId,
        eventIdOrTransactionId: EventIdOrTransactionId,
        onOpenMention: OpenMentionCallback,
    ): RoomMessageTimelineElementViewModel.FileBased.Video? =
        VideoRoomMessageTimelineElementViewModelImpl(
            viewModelContext,
            content,
            roomId,
            eventIdOrTransactionId,
            onOpenMention,
        )

    override val supports: KClass<FileBased.Video>
        get() = FileBased.Video::class

    companion object : VideoRoomMessageTimelineElementViewModelFactory
}

class VideoRoomMessageTimelineElementViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    content: FileBased.Video,
    roomId: RoomId,
    eventIdOrTransactionId: EventIdOrTransactionId,
    onOpenMention: OpenMentionCallback,
) :
    RoomMessageTimelineElementViewModel.FileBased.Video,
    FileBasedRoomMessageTimelineElementViewModel<FileBased.Video>(
        viewModelContext,
        content,
        roomId,
        eventIdOrTransactionId,
        onOpenMention,
    ) {
    override val duration: Long? = content.info?.duration

    private val thumbnails = get<Thumbnails>()

    private val thumbnailProgressFlow = MutableStateFlow<FileTransferProgress?>(null)

    override val maxAutoDownloadSize: Long = get<MatrixMessengerConfiguration>().downloadLimits.image

    override val thumbnail: StateFlow<ByteArray?> =
        combine(loadMediaResultBytes, downloadMediaResult) {
                thumbnails.loadThumbnail(
                    coroutineScope,
                    matrixClient,
                    content,
                    thumbnailProgressFlow,
                    maxMediaSizeInMemory,
                    maxAutoDownloadSize,
                )
            }
            .stateIn(coroutineScope, whileSubscribedWithTimeout, null)

    override val width: Int? = content.info?.width
    override val height: Int? = content.info?.height
}
