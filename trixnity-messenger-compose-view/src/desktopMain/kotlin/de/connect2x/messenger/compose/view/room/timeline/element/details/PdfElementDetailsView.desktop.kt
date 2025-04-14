package de.connect2x.messenger.compose.view.room.timeline.element.details

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.TransformableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
import de.connect2x.messenger.compose.view.VerticalScrollbar
import de.connect2x.messenger.compose.view.common.CenteredElement
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ThemedProgressIndicator
import de.connect2x.messenger.compose.view.theme.messengerDpConstants
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.media.PlatformMedia
import net.folivo.trixnity.client.media.okio.OkioPlatformMedia
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import kotlin.math.max
import kotlin.math.min

private val log = KotlinLogging.logger { }

@Composable
actual fun PDFReader(
    media: PlatformMedia,
    scale: Float,
    isZooming: Boolean,
    offset: MutableState<Offset>,
    state: TransformableState,
    onError: (String?) -> Unit,
) {
    val i18n = DI.current.get<I18nView>()
    val pageCacheSize = max(2f, min(16f, 8f / scale)).toInt()
    var document by remember { mutableStateOf<Pair<PDDocument, PDFRenderer>?>(null) }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    var documentWidth: Int? by remember { mutableStateOf(null) }

    val renderCache by remember {
        mutableStateOf<MutableMap<String, Pair<Long, ImageBitmap>>>(mutableMapOf())
    }
    val (temporaryFile, setTemporaryFile) = remember { mutableStateOf<OkioPlatformMedia.TemporaryFile?>(null) }

    LaunchedEffect(Unit) {
        val temporaryFileResult = (media as OkioPlatformMedia).getTemporaryFile()
        if (temporaryFileResult.isSuccess) {
            val newTemporaryFile = temporaryFileResult.getOrThrow()
            try {
                val documentData = Loader.loadPDF(newTemporaryFile.path.toFile())
                val renderer = PDFRenderer(documentData)
                document = Pair(documentData, renderer)
                documentWidth = renderer.renderImage(0)?.width
                setTemporaryFile(newTemporaryFile)
            } catch (exception: Exception) {
                onError(exception.message)
            }
        } else {
            onError(temporaryFileResult.exceptionOrNull()?.message)
        }
    }
    DisposableEffect(Unit) {
        @OptIn(DelicateCoroutinesApi::class)
        onDispose {
            GlobalScope.launch { temporaryFile?.delete() }
            document?.first?.close()
            renderCache.clear()
            document = null
        }
    }

    val density = LocalDensity.current.density
    val lazyListState = rememberLazyListState()
    val horizontalScroll = rememberScrollState()

    LaunchedEffect(offset.value) {
        lazyListState.scrollBy(-offset.value.y)
        horizontalScroll.scrollBy(-offset.value.x)
        offset.value = Offset.Zero
    }

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { viewSize = it },
        contentAlignment = Alignment.Center,
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
                    .fillMaxSize()
                    .transformable(state),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.messengerDpConstants.small),
                contentPadding = PaddingValues(horizontal = MaterialTheme.messengerDpConstants.middle),
                state = lazyListState,
                content = {
                    items(count = documentData.numberOfPages, key = { it }) { pageId ->
                        val cacheKey = "$pageId:${newDpi.toInt()}"
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
                                    .subList(0, 0.coerceAtLeast(renderCache.size - pageCacheSize))
                                    .forEach { renderCache.remove(it.first) }
                                img
                            }
                        Image(
                            bitmap = img,
                            contentDescription = i18n.fileOverlayPdfPageDescriptor(pageId),
                            modifier = Modifier
                                .background(color = Color.White) // Avoid performance drops on transparent images.
                                .width(viewSize.width.dp / density * scale - MaterialTheme.messengerDpConstants.middle * 2),
                            contentScale = ContentScale.FillWidth,
                        )
                    }
                }
            )
        } else CenteredElement {
            ThemedProgressIndicator(
                style = MaterialTheme.components.smallCircularProgressIndicator
            )
        }
        HorizontalScrollbar(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            horizontalScroll,
        )
        VerticalScrollbar(Modifier.align(Alignment.CenterEnd).fillMaxHeight(), lazyListState, false)
    }
}
