package de.connect2x.trixnity.messenger.compose.view.room.timeline.element.details

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.client.media.PlatformMedia
import de.connect2x.trixnity.client.media.okio.OkioPlatformMedia
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import org.koin.core.module.Module
import org.koin.dsl.module

private val log: Logger =
    Logger("de.connect2x.trixnity.messenger.compose.view.room.timeline.element.details.PdfElementDetailsViewKt")

actual fun getPlatformPdfReaderModule(): Module {
    return module {
        single<PDFReaderFactory> {
            object : PDFReaderFactory {
                val coroutineScope: CoroutineScope = get()

                override suspend fun create(media: PlatformMedia, onError: (String?) -> Unit): PDFReader {
                    return DesktopPDFReader(media, coroutineScope, onError).also { it.initialize() }
                }
            }
        }
    }
}

class DesktopPDFReader(
    val media: PlatformMedia,
    private val coroutineScope: CoroutineScope,
    val onError: (String?) -> Unit,
) : PDFReader {
    private val document = MutableStateFlow<Pair<PDDocument, PDFRenderer>?>(null)
    override val documentWidth: MutableState<Int?> = mutableStateOf(null)
    private val temporaryFile: MutableStateFlow<OkioPlatformMedia.TemporaryFile?> = MutableStateFlow(null)

    override val numOfPages: MutableState<Int?> = mutableStateOf(null)

    suspend fun initialize() {
        log.debug { "loading pdf..." }
        val temporaryFileResult = (media as OkioPlatformMedia).getTemporaryFile()
        if (temporaryFileResult.isSuccess) {
            try {
                val newTemporaryFile = temporaryFileResult.getOrThrow()
                log.debug { "trying to initiate pdf" }
                val documentData = Loader.loadPDF(newTemporaryFile.path.toFile())
                log.debug { "successfully loaded ${documentData.numberOfPages} pages" }
                val renderer = PDFRenderer(documentData)
                document.value = Pair(documentData, renderer)
                documentWidth.value = renderer.renderImage(0)?.width
                temporaryFile.value = newTemporaryFile
                numOfPages.value = documentData.numberOfPages
            } catch (exception: Exception) {
                log.error { "something went wrong with opening the pdf" }
                onError(exception.message)
            }
        } else {
            log.error { "cannot load pdf file into temporary file" }
            onError(temporaryFileResult.exceptionOrNull()?.message)
        }
    }

    override fun onDispose() {
        coroutineScope.launch {
            withContext(NonCancellable) {
                temporaryFile.value?.delete()
            }
        }
        document.value?.first?.close()
        document.value = null
    }

    override suspend fun getPage(pageId: Int, dpi: Float): ImageBitmap? {
        val renderer = document.first { it != null }?.second
        return renderer?.renderImageWithDPI(pageId, dpi)?.let {
            log.debug {
                "render pdf page $pageId " + "to bitmap (${it.width}x${it.height}) " + "at scale factor: $dpi "
            }
            it.toComposeImageBitmap()
        }
    }
}
