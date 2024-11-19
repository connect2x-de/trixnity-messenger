package de.connect2x.trixnity.messenger.viewmodel.files

import MediaViewModel
import MediaViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.OpenMediaType
import de.connect2x.trixnity.messenger.viewmodel.util.previewImageByteArray
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.utils.toByteArray


interface ImageViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        content: RoomMessageEventContent.FileBased.Image,
        onCloseImage: () -> Unit,
    ): ImageViewModel = ImageViewModelImpl(
        viewModelContext,
        content,
        onCloseImage,
    )

    companion object : ImageViewModelFactory
}

interface ImageViewModel : MediaViewModel {
    val image: StateFlow<ByteArray?>
}

open class ImageViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    content: RoomMessageEventContent.FileBased.Image,
    override val onCloseMedia: () -> Unit,
) : MediaViewModelImpl(
    viewModelContext,
    content,
    OpenMediaType.IMAGE,
    onCloseMedia,
), ImageViewModel {
    override val image =
        mediaDataFlow.map { it?.toByteArray() }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
}

class PreviewImageViewModel : ImageViewModel {
    override val onCloseMedia: () -> Unit = {}
    override val mediaDataFlow = MutableStateFlow(null)
    override val mediaType = OpenMediaType.IMAGE
    override val image = MutableStateFlow(previewImageByteArray())
    override val error = MutableStateFlow<String?>(null)
    override val progress = MutableStateFlow(null)
    override val fileName = "image.png"
    override val fileSize: Long? = 0
    override fun cancelMediaDownload() {}
    override fun closeMedia() {}
}
