package de.connect2x.trixnity.messenger.compose.view.room.timeline.element.details

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import de.connect2x.trixnity.client.media.PlatformMedia
import de.connect2x.trixnity.client.media.indexeddb.IndexeddbPlatformMedia
import de.connect2x.trixnity.client.media.opfs.OpfsPlatformMedia
import de.connect2x.trixnity.messenger.compose.view.files.PdfReaderWeb
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.module.Module
import org.koin.dsl.module
import pdfjs.GlobalWorkerOptions
import web.blob.Blob

actual fun getPlatformPdfReaderModule(): Module {
    return module {
        single<PDFReaderFactory> {
            object : PDFReaderFactory {
                val coroutineScope: CoroutineScope = get()

                override suspend fun create(media: PlatformMedia, onError: (String?) -> Unit): PDFReader {
                    return WebPDFReader(media, coroutineScope, onError).also { it.initialize() }
                }
            }
        }
    }
}

class WebPDFReader(
    val media: PlatformMedia,
    private val coroutineScope: CoroutineScope,
    val onError: (String?) -> Unit,
) : PDFReader {

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
            val newTemporaryFile =
                when (val result = temporaryFileResult.getOrNull()) {
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
                numOfPages.value = createdReader?.pageSize
                documentWidth.value = createdReader?.documentWidth
            } catch (exception: Throwable) {
                onError(exception.message)
            }
        } else {
            onError(temporaryFileResult.exceptionOrNull()?.message)
        }
    }

    override suspend fun getPage(pageId: Int, dpi: Float): ImageBitmap? {
        val reader = reader.filterNotNull().first()
        return reader.renderPage(pageId + 1, dpi)
    }

    override fun onDispose() {
        coroutineScope.launch {
            withContext(NonCancellable) {
                fileDeleteFunction.value?.invoke()
            }
        }
    }
}
