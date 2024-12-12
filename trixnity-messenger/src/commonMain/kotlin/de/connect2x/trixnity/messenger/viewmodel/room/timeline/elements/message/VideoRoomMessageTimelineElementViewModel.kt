package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message

import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.util.ImageSizeComputations
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EventIdOrTransactionId
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.Thumbnails
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.whileSubscribedWithTimeout
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import net.folivo.trixnity.clientserverapi.model.media.FileTransferProgress
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.FileBased
import org.koin.core.component.get
import kotlin.reflect.KClass

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
        )

    override val supports: KClass<FileBased.Video>
        get() = FileBased.Video::class

    companion object : VideoRoomMessageTimelineElementViewModelFactory
}

class VideoRoomMessageTimelineElementViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    content: FileBased.Video,
) : RoomMessageTimelineElementViewModel.FileBased.Video,
    FileBasedRoomMessageTimelineElementViewModel<FileBased.Video>(viewModelContext, content) {

    private val maxPreviewSize = get<MatrixMessengerConfiguration>().maxMediaSizeInMemory
    private val thumbnails = get<Thumbnails>()

    private val thumbnailProgressFlow = MutableStateFlow<FileTransferProgress?>(null)

    private val thumbnailLoad = coroutineScope.async { // TODO needs some sort of retry
        thumbnails.loadThumbnail(matrixClient, content, thumbnailProgressFlow, maxPreviewSize)
    }

    override val duration: Long? = content.info?.duration
    override val thumbnail: StateFlow<ByteArray?> = flow {
        emit(thumbnailLoad.await())
    }.stateIn(coroutineScope, whileSubscribedWithTimeout, null)

    override val width: Int? = content.info?.width
    override val height: Int? = content.info?.height

    override fun getDimensions(maxWidth: Int, maxHeight: Int): Pair<Int, Int>? =
        if (width == null || height == null) null
        else ImageSizeComputations.getHeight(width, maxWidth, height, maxHeight) to
                ImageSizeComputations.getWidth(width, maxWidth, height, maxHeight)
}
