package de.connect2x.trixnity.messenger.viewmodel.files

import MediaViewModel
import MediaViewModelImpl
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.OpenModalType
import de.connect2x.trixnity.messenger.viewmodel.util.limitedByteArrayOrNull
import de.connect2x.trixnity.messenger.viewmodel.util.previewImageByteArray
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.folivo.trixnity.core.model.events.m.room.EncryptedFile
import org.koin.core.component.get


interface ImageViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        mxcUrl: String,
        encryptedFile: EncryptedFile?,
        fileName: String,
        fileSize: Long?,
        onCloseImage: () -> Unit,
    ): ImageViewModel = ImageViewModelImpl(
        viewModelContext,
        mxcUrl,
        encryptedFile,
        fileName,
        fileSize,
        onCloseImage,
    )

    companion object : ImageViewModelFactory
}

interface ImageViewModel : MediaViewModel {
    val image: StateFlow<ByteArray?>
}

open class ImageViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    mxcUrl: String,
    encryptedFile: EncryptedFile?,
    override val fileName: String,
    override val fileSize: Long?,
    override val onCloseMedia: () -> Unit,
) : MediaViewModelImpl(
    viewModelContext,
    mxcUrl,
    encryptedFile,
    fileName,
    fileSize,
    OpenModalType.IMAGE,
    onCloseMedia,
), ImageViewModel {
    private val i18n = get<I18n>()
    private val maxPreviewSize = get<MatrixMessengerConfiguration>().maxMediaSizeInMemory
    override val image = mediaDataFlow.map {
        it?.limitedByteArrayOrNull(maxPreviewSize, fileSize) {
            progress.value = null
            error.value = i18n.mediaTooLargeForPreview()
        }
    }.stateIn(coroutineScope, WhileSubscribed(), null)
}

class PreviewImageViewModel : ImageViewModel {
    override val onCloseMedia: () -> Unit = {}
    override val mediaDataFlow = MutableStateFlow(null)
    override val mediaType = OpenModalType.IMAGE
    override val image = MutableStateFlow(previewImageByteArray())
    override val error = MutableStateFlow<String?>(null)
    override val progress = MutableStateFlow(null)
    override val fileName = "image.png"
    override val fileSize: Long? = 0
    override fun cancelMediaDownload() {}
    override fun closeMedia() {}
}
