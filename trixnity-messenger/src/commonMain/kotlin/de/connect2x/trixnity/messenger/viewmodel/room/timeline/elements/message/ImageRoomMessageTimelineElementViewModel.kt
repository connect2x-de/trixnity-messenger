package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message

import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EventIdOrTransactionId
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.Thumbnails
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.whileSubscribedWithTimeout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import net.folivo.trixnity.clientserverapi.model.media.FileTransferProgress
import net.folivo.trixnity.core.MSC2448
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.FileBased
import org.koin.core.component.get
import kotlin.reflect.KClass

interface ImageRoomMessageTimelineElementViewModelFactory : TimelineElementViewModelFactory<FileBased.Image> {
    override fun create(
        viewModelContext: MatrixClientViewModelContext,
        content: FileBased.Image,
        roomId: RoomId,
        eventIdOrTransactionId: EventIdOrTransactionId,
        onOpenMention: OpenMentionCallback,
    ): RoomMessageTimelineElementViewModel.FileBased.Image? =
        ImageRoomMessageTimelineElementViewModelImpl(
            viewModelContext,
            content,
        )

    override val supports: KClass<FileBased.Image>
        get() = FileBased.Image::class

    companion object : ImageRoomMessageTimelineElementViewModelFactory
}

class ImageRoomMessageTimelineElementViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    content: FileBased.Image,
) : RoomMessageTimelineElementViewModel.FileBased.Image,
    FileBasedRoomMessageTimelineElementViewModel<FileBased.Image>(viewModelContext, content) {

    private val thumbnails = get<Thumbnails>()

    private val thumbnailProgressFlow = MutableStateFlow<FileTransferProgress?>(null)
    private val maxMediaSizeInMemory = get<MatrixMessengerConfiguration>().maxMediaSizeInMemory

    private val _thumbnailLoading = MutableStateFlow(true)

    override val thumbnailLoading: StateFlow<Boolean> = _thumbnailLoading.asStateFlow()

    override val thumbnailWidth = content.info?.thumbnailInfo?.width
    override val thumbnailHeight = content.info?.thumbnailInfo?.height

    override val thumbnail: StateFlow<ByteArray?> = flow {
        emit(
            // TODO needs some sort of retry!
            thumbnails.loadThumbnail(coroutineScope, matrixClient, content, thumbnailProgressFlow, maxMediaSizeInMemory)
                .also { _thumbnailLoading.value = false }
        )
    }.stateIn(coroutineScope, whileSubscribedWithTimeout, null)

    override val width: Int? = content.info?.width
    override val height: Int? = content.info?.height

    @MSC2448
    override val blurhash: String? = content.info?.blurhash
}
