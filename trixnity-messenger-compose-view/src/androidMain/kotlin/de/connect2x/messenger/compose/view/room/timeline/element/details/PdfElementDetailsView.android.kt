package de.connect2x.messenger.compose.view.room.timeline.element.details

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.os.ParcelFileDescriptor.MODE_READ_ONLY
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.HorizontalScrollbar
import de.connect2x.messenger.compose.view.common.CenteredElement
import de.connect2x.messenger.compose.view.i18n.I18nView
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.media.PlatformMedia
import net.folivo.trixnity.client.media.okio.OkioPlatformMedia
import simpleVerticalScrollbar
import kotlin.math.max
import kotlin.math.min

private val log = KotlinLogging.logger {}

@Composable
actual fun PDFReader(
    media: PlatformMedia,
    scale: Float,
    onError: (String?) -> Unit,
) {
    val i18nView = DI.current.get<I18nView>()
    var reader by remember { mutableStateOf<PdfRender?>(null) }
    val density = LocalDensity.current.density
    var viewSize by remember { mutableStateOf(IntSize.Zero) }

    val (temporaryFile, setTemporaryFile) = remember { mutableStateOf<OkioPlatformMedia.TemporaryFile?>(null) }
    LaunchedEffect(Unit) {
        val temporaryFileResult = (media as OkioPlatformMedia).getTemporaryFile()
        if (temporaryFileResult.isSuccess) {
            val newTemporaryFile = temporaryFileResult.getOrThrow()
            try {
                setTemporaryFile(newTemporaryFile)
                reader = PdfRender(
                    fileDescriptor = ParcelFileDescriptor.open(newTemporaryFile.path.toFile(), MODE_READ_ONLY),
                )
            } catch (exception: Exception) {
                onError(exception.message)
            }
        } else {
            onError(temporaryFileResult.exceptionOrNull()?.message)
        }
    }
    LaunchedEffect(reader?.documentWidth, viewSize.width, scale, density) {
        reader?.documentWidth?.toFloat()?.let {
            val maxDpi = 1f / it * 1800f
            reader?.dpi = (viewSize.width / it * scale / density * 2f).coerceAtMost(maxDpi)
        }
    }
    DisposableEffect(Unit) {
        @OptIn(DelicateCoroutinesApi::class)
        onDispose {
            GlobalScope.launch { temporaryFile?.delete() }
            reader?.close()
        }
    }

    var readerError by remember { mutableStateOf<String?>(null) }

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { viewSize = it }
    ) {
        if (readerError == null) {
            reader?.let { pdfReader ->
                if (pdfReader.pageCount == 0) {
                    CenteredElement {
                        Text(i18nView.fileOverlayPreviewNotSupported())
                    }
                    return@let
                }
                if (pdfReader.documentWidth != null) {
                    val lazyListState = rememberLazyListState()
                    val horizontalScroll = rememberScrollState()
                    LazyColumn(
                        modifier = Modifier
                            .horizontalScroll(horizontalScroll)
                            .simpleVerticalScrollbar(lazyListState, MaterialTheme.colorScheme.primary)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(8.dp),
                        state = lazyListState,
                        content = {
                            items(count = pdfReader.pageCount, key = { it }) { pageId ->
                                pdfReader[pageId]?.pageContent?.let { img ->
                                    Image(
                                        bitmap = img,
                                        contentDescription = i18nView.fileOverlayPdfPageDescriptor(pageId),
                                        modifier = Modifier
                                            .background(color = Color.White) // Avoid performance drops on transparent images.
                                            .width(viewSize.width.dp / density * scale - 16.dp),
                                        contentScale = ContentScale.FillWidth,
                                    )
                                }
                            }
                        }
                    )
                    HorizontalScrollbar(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth(),
                        horizontalScroll,
                    )
                }
            } ?: CenteredElement {
                CircularProgressIndicator(Modifier.size(32.dp))
            }
        } else {
            readerError?.let {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                        .background(Color.Cyan),
                ) { Text(it) }
            }
        }
    }
}

// https://medium.com/telepass-digital/how-to-show-a-pdf-with-jetpack-compose-74fc773adbd0
private class PdfRender(private val fileDescriptor: ParcelFileDescriptor) {
    private val pdfRenderer = PdfRenderer(fileDescriptor)
    val pageCount get() = pdfRenderer.pageCount

    private val pageCacheSize: Int
        get() = max(2f, min(16f, 8f / (dpi ?: 1f))).toInt()

    private var _dpi: Float? = null
    var dpi: Float?
        get() = _dpi
        set(value) {
            clearPageCache()
            _dpi = value
        }

    private var pageCache = mutableMapOf<Int, Page>()
    operator fun get(pageId: Int): Page? {
        trimPageCache()
        return try {
            if (!pageCache.containsKey(pageId)) pageCache[pageId] = Page(
                pageId = pageId,
                pdfRenderer = pdfRenderer,
                dpi = _dpi ?: 1f,
                { pageCache.size },
            )
            pageCache[pageId]
        } catch (e: Exception) {
            log.error { e }
            null
        }
    }

    private fun trimPageCache() {
        pageCache.toList().sortedBy { it.second.creationTime }
            .subList(0, Math.max(0, pageCache.size - pageCacheSize))
            .forEach { pageCache.remove(it.first) }
    }

    val documentWidth: Int?
        get() = _documentWidth
            ?: this[0]?.originalWidth?.also {
                if (it > 0) _documentWidth = it
            }
    private var _documentWidth: Int? = null

    fun close() {
        clearPageCache()
        pdfRenderer.close()
        fileDescriptor.close()
    }

    private fun clearPageCache() {
        pageCache.values.forEach {
            it.recycle()
        }
        pageCache.clear()
    }

    class Page(
        val pageId: Int,
        private val pdfRenderer: PdfRenderer,
        private val dpi: Float,
        private val getCacheSize: () -> Int,
    ) {
        val pageContent get() = _pageContent.asImageBitmap()
        private val _pageContent = createBitmap()
        val creationTime = System.currentTimeMillis()
        var originalWidth: Int = 0

        private fun createBitmap(): Bitmap {
            val newBitmap: Bitmap
            pdfRenderer.openPage(pageId).use { currentPage ->
                log.debug {
                    "render pdf page $pageId " +
                            "to bitmap (${currentPage.width}x${currentPage.height}) " +
                            "at scale factor: $dpi " +
                            "with ${getCacheSize()} pages already cached"
                }
                originalWidth = currentPage.width
                newBitmap = createBlankBitmap(
                    width = (currentPage.width * dpi).toInt(),
                    height = (currentPage.height * dpi).toInt(),
                )
                currentPage.render(
                    newBitmap, null, null,
                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY,
                )
            }
            return newBitmap
        }

        fun recycle() {
            _pageContent.recycle()
        }

        private fun createBlankBitmap(
            width: Int,
            height: Int,
        ): Bitmap = androidx.core.graphics.createBitmap(
            width,
            height,
            Bitmap.Config.ARGB_8888,
        ).apply {
            val canvas = Canvas(this)
            canvas.drawBitmap(this, 0f, 0f, null)
        }
    }
}
