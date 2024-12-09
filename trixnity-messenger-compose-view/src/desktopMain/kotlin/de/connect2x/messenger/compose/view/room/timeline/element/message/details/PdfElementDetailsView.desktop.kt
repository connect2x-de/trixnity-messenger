package de.connect2x.messenger.compose.view.room.timeline.element.message.details

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.HorizontalScrollbar
import de.connect2x.messenger.compose.view.common.CenteredElement
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import net.folivo.trixnity.utils.toByteArray
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import simpleVerticalScrollbar
import kotlin.math.max
import kotlin.math.min

private val log = KotlinLogging.logger { }

@Composable
actual fun PDFReader(
    element: RoomMessageTimelineElementViewModel.FileBased.File,
    scale: Float
) {
    val i18n = DI.current.get<I18nView>()
    val pageCacheSize = max(2f, min(16f, 8f / scale)).toInt()
    val media = element.media.collectAsState()
    val filename = element.name
    var document by remember { mutableStateOf<Pair<PDDocument, PDFRenderer>?>(null) }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    var documentWidth: Int? by remember { mutableStateOf(null) }

    val renderCache by remember {
        mutableStateOf<MutableMap<String, Pair<Long, ImageBitmap>>>(mutableMapOf())
    }

    LaunchedEffect(Unit) {
        element.loadMedia(inMemory = true)
    }

    DisposableEffect(Unit) {
        onDispose {
            document?.first?.close()
            renderCache.clear()
            document = null
        }
    }

    LaunchedEffect(document) {
        if (document == null)
            media.value?.let { platformMedia ->
                val bytes = platformMedia.toByteArray()
                val documentData = org.apache.pdfbox.Loader.loadPDF(bytes)
                document = Pair(documentData, PDFRenderer(documentData))
            }
        else if (documentWidth == null) {
            documentWidth = document?.second?.renderImage(0)?.width
        }
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
        if (documentData?.numberOfPages == 0) CenteredElement {
            Text(i18n.fileOverlayPreviewNotSupported())
        }
        else if (viewSize != IntSize.Zero
            && documentWidth != null
            && documentData != null
            && renderer != null
        ) {
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
                            contentDescription = i18n.fileOverlayPdfPageDescriptor(pageId),
                            modifier = Modifier
                                .background(color = Color.White) // Avoid performance drops on transparent images.
                                .width(viewSize.width.dp / density * scale - 16.dp),
                            contentScale = ContentScale.FillWidth,
                        )
                    }
                }
            )
        } else CenteredElement {
            CircularProgressIndicator(Modifier.size(32.dp))
        }
        HorizontalScrollbar(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            horizontalScroll,
        )
    }
}
