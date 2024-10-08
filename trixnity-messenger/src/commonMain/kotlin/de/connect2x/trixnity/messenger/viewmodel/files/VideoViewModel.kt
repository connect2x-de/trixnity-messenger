package de.connect2x.trixnity.messenger.viewmodel.files

import MediaViewModel
import MediaViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.OpenModalType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.folivo.trixnity.core.model.events.m.room.EncryptedFile
import net.folivo.trixnity.utils.ByteArrayFlow


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
    override val video = mediaDataFlow
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
