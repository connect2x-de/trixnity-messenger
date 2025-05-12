package de.connect2x.messenger.compose.view.room.timeline.element.details

import android.graphics.pdf.PdfRenderer
import android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
import android.os.ParcelFileDescriptor
import android.os.ParcelFileDescriptor.MODE_READ_ONLY
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.media.PlatformMedia
import net.folivo.trixnity.client.media.okio.OkioPlatformMedia
import kotlin.coroutines.coroutineContext

private val log = KotlinLogging.logger {}

actual suspend fun getPlatformPDFReader(media: PlatformMedia, onError: (String?) -> Unit): PDFReader {
    val reader = PDFPlatformReader(media, onError)
    reader.initialize()
    return reader
}

class PDFPlatformReader(val media: PlatformMedia, val onError: (String?) -> Unit) : PDFReader {
    override val numOfPages: MutableState<Int?> = mutableStateOf(null)
    override val documentWidth: MutableState<Int?> = mutableStateOf(null)

    private val temporaryFile: MutableStateFlow<OkioPlatformMedia.TemporaryFile?> = MutableStateFlow(null)
    private val renderer = MutableStateFlow<PdfRenderer?>(null)

    suspend fun initialize() {
        val temporaryFileResult = (media as OkioPlatformMedia).getTemporaryFile()
        if (temporaryFileResult.isSuccess) {
            val newTemporaryFile = temporaryFileResult.getOrThrow()
            try {
                temporaryFile.value = newTemporaryFile
                renderer.value = PdfRenderer(ParcelFileDescriptor.open(newTemporaryFile.path.toFile(), MODE_READ_ONLY))
                documentWidth.value = renderer.value?.openPage(0).use { it?.width }
                numOfPages.value = renderer.value?.pageCount
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
    ): Deferred<ImageBitmap?> = CoroutineScope(coroutineContext).async {
        renderer.value?.let {
            it.openPage(pageId).use { page ->
                val scaledDpi = dpi.div(72f)
                val width = (page.width * scaledDpi).toInt()
                val height = (page.height * scaledDpi).toInt()
                val bitmap = createBitmap(width, height)
                log.debug {
                    "render pdf page $pageId " +
                            "to viewport (${width}x${height}) " +
                            "at scale factor: $dpi "
                }
                page.render(bitmap, null, null, RENDER_MODE_FOR_DISPLAY)
                return@async bitmap.asImageBitmap()
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onDispose() {
        GlobalScope.launch { temporaryFile.value?.delete() }
        renderer.value?.close()
    }
}
