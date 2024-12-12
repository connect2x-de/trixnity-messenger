package de.connect2x.trixnity.messenger.viewmodel.files

import MediaViewModel
import MediaViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.OpenMediaType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.utils.ByteArrayFlow


interface VideoViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        content: RoomMessageEventContent.FileBased.Video,
        onCloseVideo: () -> Unit,
        onDownload: () -> Unit,
    ): VideoViewModel = VideoViewModelImpl(
        viewModelContext,
        content,
        onCloseVideo,
        onDownload
    )

    companion object : VideoViewModelFactory
}

interface VideoViewModel : MediaViewModel {
    val video: StateFlow<ByteArrayFlow?>
}

open class VideoViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    content: RoomMessageEventContent.FileBased.Video,
    override val onCloseMedia: () -> Unit,
    onDownload: () -> Unit,
) : MediaViewModelImpl(
    viewModelContext,
    content,
    OpenMediaType.VIDEO,
    onCloseMedia,
    onDownload
), VideoViewModel {
    override val video = mediaDataFlow
}

class PreviewVideoViewModel : VideoViewModel {
    override val onCloseMedia: () -> Unit = {}
    override val mediaDataFlow = MutableStateFlow(null) // TODO: video data
    override val video = mediaDataFlow
    override val error = MutableStateFlow<String?>(null)
    override val mediaType = OpenMediaType.VIDEO
    override val progress = MutableStateFlow(null)
    override val fileName = "video.png"
    override val fileSize: Long? = 0
    override fun cancelMediaDownload() {}
    override fun closeMedia() {}
    override fun downloadMedia() {}
}
