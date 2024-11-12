package de.connect2x.messenger.compose.view.files

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.HorizontalScrollbar
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.files.PdfDocumentViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import simpleVerticalScrollbar
import kotlin.math.max
import kotlin.math.min

private val log = KotlinLogging.logger { }

@Composable
actual fun PDFReader(documentViewModel: PdfDocumentViewModel, scale: Float) {
    val i18nView = DI.current.get<I18nView>()
    val pageCacheSize = max(2f, min(16f, 8f / scale)).toInt()
    val media = documentViewModel.document.collectAsState()
    val error = documentViewModel.error.collectAsState()
    val filename = documentViewModel.fileName
    var document by remember { mutableStateOf<Pair<PDDocument, PDFRenderer>?>(null) }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    var documentWidth: Int? by remember { mutableStateOf(null) }

    val errorText = error.value
    if (errorText != null) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize().padding(32.dp),
        ) { Text(errorText) }
        return
    }

    val renderCache by remember {
        mutableStateOf<MutableMap<String, Pair<Long, ImageBitmap>>>(mutableMapOf())
    }

    DisposableEffect(Unit) {
        onDispose {
            document?.first?.close()
            renderCache.clear()
            document = null
        }
    }

    if (document == null)
        media.value?.let { bytes ->
            val documentData = org.apache.pdfbox.Loader.loadPDF(bytes)
            document = Pair(documentData, PDFRenderer(documentData))
        }
    else if (documentWidth == null) {
        documentWidth = document?.second?.renderImage(0)?.width
    }

    val density = LocalDensity.current.density
    val lazyListState = rememberLazyListState()
    val horizontalScroll = rememberScrollState()

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { viewSize = it }
    ) {
        val documentData = document?.first
        val renderer = document?.second
        if (viewSize != IntSize.Zero && documentWidth != null && documentData != null && renderer != null) {
            val dwidth: Float = documentWidth?.toFloat() ?: 1f
            val maxDpi = 1f / dwidth * 64f * 3600f
            val newDpi = (viewSize.width / dwidth * scale / density * 64f).coerceAtMost(maxDpi)
            LazyColumn(
                modifier = Modifier
                    .horizontalScroll(horizontalScroll)
                    .simpleVerticalScrollbar(lazyListState, MaterialTheme.colorScheme.primary)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(8.dp),
                state = lazyListState,
                content = {
                    items(count = documentData.numberOfPages, key = { it }) { pageId ->
                        val cacheKey = "$filename:$pageId:${newDpi.toInt()}"
                        val img = renderCache[cacheKey]?.second
                            ?: renderer.renderImageWithDPI(pageId, newDpi).let {
                                val img = it.toComposeImageBitmap()
                                log.debug {
                                    "render pdf page $pageId " +
                                            "to bitmap (${img.width}x${img.height}) " +
                                            "at scale factor: $newDpi " +
                                            "with ${renderCache.size} pages already cached"
                                }
                                renderCache[cacheKey] = Pair<Long, ImageBitmap>(System.currentTimeMillis(), img)
                                renderCache.toList().sortedBy { it.second.first }
                                    .subList(0, Math.max(0, renderCache.size - pageCacheSize))
                                    .forEach { renderCache.remove(it.first) }
                                img
                            }
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
            )
        } else Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            CircularProgressIndicator(Modifier.size(32.dp))
        }
        HorizontalScrollbar(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            horizontalScroll,
        )
    }
}
