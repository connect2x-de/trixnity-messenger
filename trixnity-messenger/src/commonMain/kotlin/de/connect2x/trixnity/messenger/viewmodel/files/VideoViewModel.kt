package de.connect2x.trixnity.messenger.viewmodel.files

import MediaViewModel
import MediaViewModelImpl
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.OpenMediaType
import de.connect2x.trixnity.messenger.viewmodel.util.MaxByteFlowSizeException
import de.connect2x.trixnity.messenger.viewmodel.util.limitSize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.utils.ByteArrayFlow
import org.koin.core.component.get


interface VideoViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        content: RoomMessageEventContent.FileBased.Video,
        onCloseVideo: () -> Unit,
    ): VideoViewModel = VideoViewModelImpl(
        viewModelContext,
        content,
        onCloseVideo,
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
) : MediaViewModelImpl(
    viewModelContext,
    content,
    OpenMediaType.VIDEO,
    onCloseMedia,
), VideoViewModel {
    val maxPreviewSize = get<MatrixMessengerConfiguration>().maxMediaSizeInMemory
    private val fileSizePrivate = fileSize
    override val video =
        if (fileSizePrivate != null && fileSizePrivate <= maxPreviewSize) {
            mediaDataFlow.map {
                it?.limitSize(maxPreviewSize)?.catch { e ->
                    if (e.cause is MaxByteFlowSizeException) {
                        error.value = i18n.mediaTooLargeForPreview()
                    } else error.value = i18n.mediaCanNotBePreviewed()
                }
            }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
        } else {
            progress.value = null
            error.value = i18n.mediaTooLargeForPreview()
            MutableStateFlow(null)
        }
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
}
