package de.connect2x.messenger.compose.view.room.timeline.element.details

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.media.PlatformMedia
import net.folivo.trixnity.client.media.okio.OkioPlatformMedia
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import kotlin.coroutines.coroutineContext

private val log = KotlinLogging.logger { }

actual suspend fun getPlatformPDFReader(
    media: PlatformMedia,
    onError: (String?) -> Unit,
): PDFReader {
    val reader = PlatformPDFReader(media, onError)
    reader.initialize()

    return reader
}

class PlatformPDFReader(
    val media: PlatformMedia,
    val onError: (String?) -> Unit,
) : PDFReader {
    private val document = MutableStateFlow<Pair<PDDocument, PDFRenderer>?>(null)
    override val documentWidth: MutableState<Int?> = mutableStateOf(null)
    private val temporaryFile: MutableStateFlow<OkioPlatformMedia.TemporaryFile?> = MutableStateFlow(null)
    private val renderCache: MutableStateFlow<MutableMap<String, Pair<Long, ImageBitmap>>> =
        MutableStateFlow(mutableMapOf())


    override val numOfPages: MutableState<Int?> = mutableStateOf(null)
    suspend fun initialize() {
        val temporaryFileResult = (media as OkioPlatformMedia).getTemporaryFile()
        if (temporaryFileResult.isSuccess) {
            val newTemporaryFile = temporaryFileResult.getOrThrow()
            try {
                val documentData = Loader.loadPDF(newTemporaryFile.path.toFile())
                val renderer = PDFRenderer(documentData)
                document.value = Pair(documentData, renderer)
                documentWidth.value = renderer.renderImage(0)?.width
                temporaryFile.value = newTemporaryFile
                numOfPages.value = documentData.numberOfPages
            } catch (exception: Exception) {
                onError(exception.message)
            }
        } else {
            onError(temporaryFileResult.exceptionOrNull()?.message)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onDispose() {
        GlobalScope.launch { temporaryFile.value?.delete() }
        document.value?.first?.close()
        renderCache.value.clear()
        document.value = null
    }


    override suspend fun getPage(pageId: Int, dpi: Float): Deferred<ImageBitmap?> = CoroutineScope(coroutineContext).async {
        val renderer = document.first { it != null }?.second
        return@async renderer?.renderImageWithDPI(pageId, dpi)?.let {
            log.debug {
                "render pdf page $pageId " +
                        "to bitmap (${it.width}x${it.height}) " +
                        "at scale factor: $dpi " +
                        "with ${renderCache.value.size} pages already cached"
            }
            it.toComposeImageBitmap()
        }
    }
}
