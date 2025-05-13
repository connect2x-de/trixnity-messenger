package de.connect2x.messenger.compose.view.room.timeline.element.details

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import de.connect2x.messenger.compose.view.files.GlobalWorkerOptions
import de.connect2x.messenger.compose.view.files.PdfReaderWeb
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.media.PlatformMedia
import net.folivo.trixnity.client.media.indexeddb.IndexeddbPlatformMedia
import net.folivo.trixnity.client.media.opfs.OpfsPlatformMedia
import web.blob.Blob

actual suspend fun getPlatformPDFReader(media: PlatformMedia, onError: (String?) -> Unit): PDFReader {
    val reader = PDFPlatformReader(media, onError)
    reader.initialize()
    return reader
}

class PDFPlatformReader(val media: PlatformMedia, val onError: (String?) -> Unit) : PDFReader {

    private val fileDeleteFunction: MutableStateFlow<(suspend () -> Unit)?> = MutableStateFlow(null)
    private val temporaryFile: MutableStateFlow<Blob?> = MutableStateFlow(null)
    private val reader: MutableStateFlow<PdfReaderWeb?> = MutableStateFlow(null)
    override val numOfPages: MutableState<Int?> = mutableStateOf(null)
    override val documentWidth: MutableState<Int?> = mutableStateOf(null)

    suspend fun initialize() {
        GlobalWorkerOptions.workerSrc = "./pdf.worker.mjs"
        val temporaryFileResult =
            (media as? OpfsPlatformMedia)?.getTemporaryFile() ?: (media as IndexeddbPlatformMedia).getTemporaryFile()
        if (temporaryFileResult.isSuccess) {
            val newTemporaryFile = when (val result = temporaryFileResult.getOrNull()) {
                is OpfsPlatformMedia.TemporaryFile -> {
                    fileDeleteFunction.value = result::delete
                    result.file
                }

                is IndexeddbPlatformMedia.TemporaryFile -> {
                    fileDeleteFunction.value = result::delete
                    result.file
                }

                else -> null
            }
            try {
                temporaryFile.value = newTemporaryFile
                val createdReader = newTemporaryFile?.let { PdfReaderWeb(it) }
                reader.value = createdReader
                numOfPages.value = createdReader?.pageSize?.first { it != null }
                documentWidth.value = createdReader?.documentWidth?.first { it != null }
            } catch (exception: Exception) {
                onError(exception.message)
            }
        } else {
            onError(temporaryFileResult.exceptionOrNull()?.message)
        }
    }

    override suspend fun getPage(
        pageId: Int,
        dpi: Float
    ): ImageBitmap? {
        val reader = reader.first { it != null }
        val renderFlow: MutableStateFlow<ImageBitmap?> = MutableStateFlow(null)
        reader?.renderPage(pageId + 1, renderFlow, dpi.div(72f))
        return renderFlow.first { it != null }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onDispose() {
        GlobalScope.launch { fileDeleteFunction.value?.invoke() }
    }
}
