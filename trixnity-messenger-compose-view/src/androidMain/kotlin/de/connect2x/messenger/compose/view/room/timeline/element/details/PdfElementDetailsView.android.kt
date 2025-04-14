package de.connect2x.messenger.compose.view.room.timeline.element.details

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
import android.os.ParcelFileDescriptor
import android.os.ParcelFileDescriptor.MODE_READ_ONLY
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
import androidx.compose.ui.graphics.asImageBitmap
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
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.media.PlatformMedia
import net.folivo.trixnity.client.media.okio.OkioPlatformMedia
import kotlin.math.max
import kotlin.math.min

private val log = KotlinLogging.logger {}

@Composable
actual fun PDFReader(
    media: PlatformMedia,
    scale: Float,
    isZooming: Boolean,
    offset: MutableState<Offset>,
    state: TransformableState,
    onError: (String?) -> Unit,
) {
    val i18nView = DI.current.get<I18nView>()
    val renderer = remember { mutableStateOf<PdfRenderer?>(null) }
    val density = LocalDensity.current.density
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    val pageCacheSize = max(2f, min(16f, 8f / scale)).toInt()
    val cache = remember {
        mutableStateOf<MutableMap<String, Pair<Long, ImageBitmap>>>(mutableMapOf())
    }
    val dpi = remember { mutableStateOf<Float?>(null) }
    val documentWidth = remember { mutableStateOf<Int?>(null) }
    val (temporaryFile, setTemporaryFile) = remember { mutableStateOf<OkioPlatformMedia.TemporaryFile?>(null) }
    LaunchedEffect(Unit) {
        val temporaryFileResult = (media as OkioPlatformMedia).getTemporaryFile()
        if (temporaryFileResult.isSuccess) {
            val newTemporaryFile = temporaryFileResult.getOrThrow()
            try {
                setTemporaryFile(newTemporaryFile)
                renderer.value = PdfRenderer(ParcelFileDescriptor.open(newTemporaryFile.path.toFile(), MODE_READ_ONLY))
                documentWidth.value = renderer.value?.openPage(0).use { it?.width }
            } catch (exception: Exception) {
                onError(exception.message)
            }
        } else {
            onError(temporaryFileResult.exceptionOrNull()?.message)
        }
    }
    LaunchedEffect(documentWidth.value, viewSize.width) {
        documentWidth.value?.toFloat()?.let {
            val maxDpi = 1f / it * 1800f
            dpi.value = (viewSize.width / it * scale / density * 2f).coerceAtMost(maxDpi)
        }
    }
    DisposableEffect(Unit) {
        @OptIn(DelicateCoroutinesApi::class)
        onDispose {
            GlobalScope.launch { temporaryFile?.delete() }
            renderer.value?.close()
        }
    }
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
        contentAlignment = Alignment.Center
    ) {
        val pdfReader = renderer.value
        val dpi = dpi.value
        if (pdfReader != null && dpi != null && pdfReader.pageCount > 0) {
            LazyColumn(
                modifier = Modifier
                    .horizontalScroll(horizontalScroll)
                    .fillMaxWidth()
                    .transformable(state),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(8.dp),
                state = lazyListState,
                userScrollEnabled = false,
                content = {
                    items(count = pdfReader.pageCount, key = { it }) { pageId ->
                        val cacheKey = "$pageId:${dpi}"
                        val currentPage = cache.value[cacheKey]?.second ?: run {
                            pdfReader.openPage(pageId).use { page ->
                                val width = (page.width * dpi).toInt()
                                val height = (page.height * dpi).toInt()
                                val bitmap = Bitmap.createBitmap(
                                    width,
                                    height,
                                    Bitmap.Config.ARGB_8888
                                )
                                log.debug {
                                    "render pdf page $pageId " +
                                            "to viewport (${width}x${height}) " +
                                            "at scale factor: $scale " +
                                            "with ${cache.value.size} pages already cached"
                                }
                                page.render(bitmap, null, null, RENDER_MODE_FOR_DISPLAY)
                                val image = bitmap.asImageBitmap()
                                cache.value[cacheKey] = Pair(System.currentTimeMillis(), image)
                                cache.value.toList().sortedBy { it.second.first }
                                    .subList(0, 0.coerceAtLeast(cache.value.size - pageCacheSize))
                                    .forEach { cache.value.remove(it.first) }
                                image
                            }
                        }

                        Image(
                            bitmap = currentPage,
                            contentDescription = i18nView.fileOverlayPdfPageDescriptor(pageId),
                            modifier = Modifier
                                .background(color = Color.White) // Avoid performance drops on transparent images.
                                .width(viewSize.width.dp / density * scale - 16.dp),
                            contentScale = ContentScale.FillWidth,
                        )
                    }
                }
            )
            HorizontalScrollbar(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                horizontalScroll,
            )
            VerticalScrollbar(
                Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight(), lazyListState, false
            )

        } //No Renderer yet -> Display a loading indicator
        else if (pdfReader == null) CenteredElement {
            ThemedProgressIndicator(
                style = MaterialTheme.components.smallCircularProgressIndicator
            )
        }
        else if (pdfReader.pageCount > 0) {
            CenteredElement {
                Text(i18nView.fileOverlayPreviewNotSupported())
            }

        }
    }
}
