package de.connect2x.trixnity.messenger.viewmodel.files

import MediaViewModel
import MediaViewModelImpl
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.OpenModalType
import de.connect2x.trixnity.messenger.viewmodel.util.MaxByteFlowSizeException
import de.connect2x.trixnity.messenger.viewmodel.util.limitSize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.folivo.trixnity.core.model.events.m.room.EncryptedFile
import net.folivo.trixnity.utils.ByteArrayFlow
import org.koin.core.component.get


interface VideoViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        mxcUrl: String,
        encryptedFile: EncryptedFile?,
        fileName: String,
        onCloseVideo: () -> Unit,
    ): VideoViewModel = VideoViewModelImpl(
        viewModelContext,
        mxcUrl,
        encryptedFile,
        fileName,
        onCloseVideo,
    )

    companion object : VideoViewModelFactory
}

interface VideoViewModel : MediaViewModel {
    val video: StateFlow<ByteArrayFlow?>
}

open class VideoViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    mxcUrl: String,
    encryptedFile: EncryptedFile?,
    override val fileName: String,
    override val onCloseMedia: () -> Unit,
) : MediaViewModelImpl(
    viewModelContext,
    mxcUrl,
    encryptedFile,
    fileName,
    OpenModalType.VIDEO,
    onCloseMedia,
), VideoViewModel {
    val maxPreviewSize = get<MatrixMessengerConfiguration>().filePreviewMaxSize
    override val video = mediaDataFlow.map {
        it?.limitSize(maxPreviewSize)?.catch { e ->
            if (e.cause is MaxByteFlowSizeException) {
                error.value = i18n.mediaTooLargeForPreview()
            } else error.value = i18n.mediaCanNotBePreviewed()
        }
    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
}

class PreviewVideoViewModel : VideoViewModel {
    override val onCloseMedia: () -> Unit = {}
    override val mediaDataFlow = MutableStateFlow(null) // TODO: video data
    override val video = mediaDataFlow
    override val error = MutableStateFlow<String?>(null)
    override val mediaType = OpenModalType.VIDEO
    override val progress = MutableStateFlow(null)
    override val fileName = "video.png"
    override fun cancelMediaDownload() {}
    override fun closeMedia() {}
}
