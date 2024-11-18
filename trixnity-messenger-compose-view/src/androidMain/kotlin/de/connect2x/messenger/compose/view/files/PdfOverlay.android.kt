package de.connect2x.messenger.compose.view.files

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
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
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.HorizontalScrollbar
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.viewmodel.media.PdfDocumentViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import net.folivo.trixnity.utils.ByteArrayFlow
import simpleVerticalScrollbar
import java.io.File
import kotlin.math.max
import kotlin.math.min


private val log = KotlinLogging.logger {}

@Composable
actual fun PDFReader(documentViewModel: PdfDocumentViewModel, scale: Float) {
    val i18n = DI.current.get<I18n>()
    val i18nView = DI.current.get<I18nView>()
    val document = documentViewModel.documentFlow.collectAsState().value
    val mediaError = documentViewModel.error.collectAsState().value
    var reader by remember { mutableStateOf<PdfRender?>(null) }
    val filename = documentViewModel.fileName
    val context = LocalContext.current
    val density = LocalDensity.current.density
    var viewSize by remember { mutableStateOf(IntSize.Zero) }

    DisposableEffect(Unit) {
        onDispose {
            reader?.close()
        }
    }

    var readerError by remember { mutableStateOf<String?>(null) }
    var forceReloadFile by remember { mutableStateOf(false) }

    if (reader == null && document != null) LaunchedEffect(filename, forceReloadFile) {
        val tempForceReload = forceReloadFile
        forceReloadFile = false
        readerError = null
        saveToCache(
            context, document, filename, tempForceReload,
            onCompletion = {
                try {
                    reader = PdfRender(
                        fileDescriptor = context.contentResolver
                            .openFileDescriptor(it, "r")!!,
                    )
                } catch (e: Exception) {
                    reader = null
                    forceReloadFile = true
                    readerError = i18n.mediaCouldNotBeRead()
                    // TODO: check file hash to avoid endless reload loops for broken files
                }
            },
            onFailure = {
                reader = null
                readerError = i18n.mediaCouldNotBeRead()
            },
        )
    }

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { viewSize = it }
    ) {
        (mediaError ?: readerError)?.let {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
                    .background(Color.Cyan),
            ) { Text(it) }
        } ?: reader?.let { pdfReader ->
            val dwidth: Float = pdfReader.documentWidth?.toFloat()
                ?: return
            val maxDpi = 1f / dwidth * 1800f
            val newDpi = (viewSize.width / dwidth * scale / density * 2f).coerceAtMost(maxDpi)
            if (pdfReader.dpi != newDpi) {
                pdfReader.dpi = newDpi
            }
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
        } ?: Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            CircularProgressIndicator(Modifier.size(32.dp))
        }
    }
}

private suspend fun saveToCache(
    context: Context,
    bytes: ByteArrayFlow,
    fileName: String,
    forceReload: Boolean = false,
    onCompletion: (Uri) -> Unit,
    onFailure: ((Throwable) -> Unit)?,
) {
    var uri: Uri? = null
    try {
        val tempFileName = "$fileName.temp"
        val file = File(context.cacheDir, tempFileName)
        file.toUri().let {
            uri = it // Keep uri reference so it can be removed on failure.
            if (!file.exists() || forceReload) {
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    // TODO: use stream.writeBytes once Trixnity SDK allows for it.
                    bytes.collect { bytes ->
                        stream.write(bytes)
                    }
                }
            }
            onCompletion(it)
        }
    } catch (e: Exception) {
        try {
            context.contentResolver.delete(uri!!, null, null)
        } catch (_: Exception) {
        }
        log.error { e }
        onFailure?.let { it(e) }
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
