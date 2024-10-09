package de.connect2x.trixnity.messenger.viewmodel.files

import MediaViewModel
import MediaViewModelImpl
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.files.MediaConstants.MAX_SIZE_DOCUMENT_PREVIEW_BYTES
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.OpenModalType
import de.connect2x.trixnity.messenger.viewmodel.util.MaxByteFlowSizeException
import de.connect2x.trixnity.messenger.viewmodel.util.limitSize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.folivo.trixnity.core.model.events.m.room.EncryptedFile
import net.folivo.trixnity.utils.ByteArrayFlow
import net.folivo.trixnity.utils.toByteArray
import org.koin.core.component.get


interface PdfDocumentViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        mxcUrl: String,
        encryptedFile: EncryptedFile?,
        fileName: String,
        onCloseDocument: () -> Unit,
    ): PdfDocumentViewModel = PdfDocumentViewModelImpl(
        viewModelContext,
        mxcUrl,
        encryptedFile,
        fileName,
        onCloseDocument,
    )

    companion object : PdfDocumentViewModelFactory
}

interface PdfDocumentViewModel : MediaViewModel {
    val document: StateFlow<ByteArray?>
    val documentFlow: StateFlow<ByteArrayFlow?>
}

open class PdfDocumentViewModelImpl(
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
    OpenModalType.PDF,
    onCloseMedia,
), PdfDocumentViewModel {
    private val i18n = get<I18n>()
    override val documentFlow = mediaDataFlow
    override val document = mediaDataFlow.map {
        it?.limitSize(MAX_SIZE_DOCUMENT_PREVIEW_BYTES)
            ?.catch { e ->
                if (e.cause is MaxByteFlowSizeException) error.value = i18n.mediaTooLargeForPreview()
                else error.value = i18n.mediaCanNotBePreviewed()
            }
            ?.toByteArray()
    }.stateIn(coroutineScope, WhileSubscribed(), null)
}

class PreviewPdfDocumentViewModel : PdfDocumentViewModel {
    override val onCloseMedia: () -> Unit = {}
    override val mediaDataFlow = MutableStateFlow(null) // TODO: document data
    override val documentFlow = mediaDataFlow
    override val document = MutableStateFlow(null) // TODO: document data
    override val error = MutableStateFlow<String?>(null)
    override val progress = MutableStateFlow(null)
    override val mediaType = OpenModalType.TEXT
    override val fileName = "document.pdf"
    override fun cancelMediaDownload() {}
    override fun closeMedia() {}
}
