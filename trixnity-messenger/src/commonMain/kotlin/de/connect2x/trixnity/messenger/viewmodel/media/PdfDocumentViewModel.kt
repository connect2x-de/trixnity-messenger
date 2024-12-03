package de.connect2x.trixnity.messenger.viewmodel.media

import MediaViewModel
import MediaViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.OpenMediaType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.utils.ByteArrayFlow
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import net.folivo.trixnity.utils.toByteArray


interface PdfDocumentViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        content: RoomMessageEventContent.FileBased.File,
        onCloseDocument: () -> Unit,
        onDownload: () -> Unit,
    ): PdfDocumentViewModel = PdfDocumentViewModelImpl(
        viewModelContext,
        content,
        onCloseDocument,
        onDownload
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
    onDownload: () -> Unit,
) : MediaViewModelImpl(
    viewModelContext,
    content,
    OpenMediaType.PDF,
    onCloseMedia,
    onDownload
), PdfDocumentViewModel {
    override val documentFlow = mediaDataFlow
    override val document = mediaDataFlow.map {
        it?.toByteArray()
    }.stateIn(coroutineScope, WhileSubscribed(), null)
}

class PreviewPdfDocumentViewModel : PdfDocumentViewModel {
    override val onCloseMedia: () -> Unit = {}

    @OptIn(ExperimentalEncodingApi::class)
    override val mediaDataFlow = MutableStateFlow(
        flowOf(
            Base64.decode(
                "JVBERi0xLjcKCjEgMCBvYmogICUgZW50cnkgcG9pbnQKPDwKICAvVHlwZSAvQ2F0YWxvZwog" +
                        "IC9QYWdlcyAyIDAgUgo+PgplbmRvYmoKCjIgMCBvYmoKPDwKICAvVHlwZSAvUGFnZXMKICAv" +
                        "TWVkaWFCb3ggWyAwIDAgMjAwIDIwMCBdCiAgL0NvdW50IDEKICAvS2lkcyBbIDMgMCBSIF0K" +
                        "Pj4KZW5kb2JqCgozIDAgb2JqCjw8CiAgL1R5cGUgL1BhZ2UKICAvUGFyZW50IDIgMCBSCiAg" +
                        "L1Jlc291cmNlcyA8PAogICAgL0ZvbnQgPDwKICAgICAgL0YxIDQgMCBSIAogICAgPj4KICA+" +
                        "PgogIC9Db250ZW50cyA1IDAgUgo+PgplbmRvYmoKCjQgMCBvYmoKPDwKICAvVHlwZSAvRm9u" +
                        "dAogIC9TdWJ0eXBlIC9UeXBlMQogIC9CYXNlRm9udCAvVGltZXMtUm9tYW4KPj4KZW5kb2Jq" +
                        "Cgo1IDAgb2JqICAlIHBhZ2UgY29udGVudAo8PAogIC9MZW5ndGggNDQKPj4Kc3RyZWFtCkJU" +
                        "CjcwIDUwIFRECi9GMSAxMiBUZgooSGVsbG8sIHdvcmxkISkgVGoKRVQKZW5kc3RyZWFtCmVu" +
                        "ZG9iagoKeHJlZgowIDYKMDAwMDAwMDAwMCA2NTUzNSBmIAowMDAwMDAwMDEwIDAwMDAwIG4g" +
                        "CjAwMDAwMDAwNzkgMDAwMDAgbiAKMDAwMDAwMDE3MyAwMDAwMCBuIAowMDAwMDAwMzAxIDAw" +
                        "MDAwIG4gCjAwMDAwMDAzODAgMDAwMDAgbiAKdHJhaWxlcgo8PAogIC9TaXplIDYKICAvUm9v" +
                        "dCAxIDAgUgo+PgpzdGFydHhyZWYKNDkyCiUlRU9G"
            )
        )
    )
    override val documentFlow = mediaDataFlow
    override val document = MutableStateFlow(null) // TODO: document data
    override val error = MutableStateFlow<String?>(null)
    override val progress = MutableStateFlow(null)
    override val mediaType = OpenMediaType.PDF
    override val fileName = "document.pdf"
    override val fileSize: Long? = 0
    override fun cancelMediaDownload() {}
    override fun closeMedia() {}
    override fun downloadMedia() {}
}
