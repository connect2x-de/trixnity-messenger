package de.connect2x.trixnity.messenger.viewmodel.files

import MediaViewModel
import MediaViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.OpenMediaType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.utils.ByteArrayFlow
import net.folivo.trixnity.utils.toByteArray


interface PdfDocumentViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        content: RoomMessageEventContent.FileBased.File,
        onCloseDocument: () -> Unit,
    ): PdfDocumentViewModel = PdfDocumentViewModelImpl(
        viewModelContext,
        content,
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
    val content: RoomMessageEventContent.FileBased.File,
    override val onCloseMedia: () -> Unit,
) : MediaViewModelImpl(
    viewModelContext,
    content,
    OpenMediaType.PDF,
    onCloseMedia,
), PdfDocumentViewModel {
    override val documentFlow = mediaDataFlow
    override val document = mediaDataFlow.map {
        it?.toByteArray()
    }.stateIn(coroutineScope, WhileSubscribed(), null)
}

class PreviewPdfDocumentViewModel : PdfDocumentViewModel {
    override val onCloseMedia: () -> Unit = {}
    override val mediaDataFlow = MutableStateFlow(null) // TODO: document data
    override val documentFlow = mediaDataFlow
    override val document = MutableStateFlow(null) // TODO: document data
    override val error = MutableStateFlow<String?>(null)
    override val progress = MutableStateFlow(null)
    override val mediaType = OpenMediaType.PDF
    override val fileName = "document.pdf"
    override val fileSize: Long? = 0
    override fun cancelMediaDownload() {}
    override fun closeMedia() {}
}
