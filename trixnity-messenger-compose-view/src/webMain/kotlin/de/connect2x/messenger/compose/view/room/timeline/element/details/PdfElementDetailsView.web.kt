package de.connect2x.messenger.compose.view.room.timeline.element.details

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.TransformableState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import coil3.compose.rememberAsyncImagePainter
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.files.GlobalWorkerOptions
import de.connect2x.messenger.compose.view.files.PdfReaderWeb
import de.connect2x.messenger.compose.view.files.toImageBitmap
import de.connect2x.messenger.compose.view.i18n.I18nView
import io.ktor.utils.io.core.toByteArray
import net.folivo.trixnity.client.media.PlatformMedia
import net.folivo.trixnity.client.media.indexeddb.IndexeddbPlatformMedia
import net.folivo.trixnity.client.media.opfs.OpfsPlatformMedia
import simpleVerticalScrollbar
import web.blob.Blob
import web.html.HTMLCanvasElement
import kotlin.math.max
import kotlin.math.min

@Composable
actual fun PDFReader(
    media: PlatformMedia,
    scale: Float,
    isZooming: Boolean,
    state: TransformableState,
    offset: Offset,
    onError: (String?) -> Unit,
) {
    val i18nView = DI.current.get<I18nView>()
    val density = LocalDensity.current.density
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    val pageCacheSize = max(2f, min(16f, 8f / scale)).toInt()
    val cache = remember {
        mutableStateOf<MutableMap<String, Pair<Long, HTMLCanvasElement>>>(mutableMapOf())
    }
    val dpi = remember { mutableStateOf<Float?>(null) }
    val documentWidth = remember { mutableStateOf<Int?>(null) }
    val (temporaryFile, setTemporaryFile) = remember { mutableStateOf<Blob?>(null) }
    val reader = remember { mutableStateOf<PdfReaderWeb?>(null) }
    GlobalWorkerOptions.workerSrc = "./pdf.worker.mjs"
    LaunchedEffect(Unit) {
        val temporaryFileResult =
            (media as? OpfsPlatformMedia)?.getTemporaryFile() ?: (media as IndexeddbPlatformMedia).getTemporaryFile()
        if (temporaryFileResult.isSuccess == true) {
            val newTemporaryFile = when (val result = temporaryFileResult.getOrNull()) {
                is OpfsPlatformMedia.TemporaryFile -> result.file
                is IndexeddbPlatformMedia.TemporaryFile -> result.file
                else -> null
            }
            try {
                setTemporaryFile(newTemporaryFile)
                val createdReader = newTemporaryFile?.let { PdfReaderWeb(it) }
                reader.value = createdReader
                //documentWidth.value = renderer.value?.openPage(0).use { it?.width }
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

    val listState = rememberLazyListState()
    reader.value?.let { reader ->
        val pageCount = reader.pageSize.value
        pageCount?.let {
            LazyColumn(
                modifier = Modifier
                    .simpleVerticalScrollbar(listState, MaterialTheme.colorScheme.primary)
                    .fillMaxWidth(),
                state = listState
            ) {
                items(it) { pageId ->
                    val currentBitmap = reader.getOrRenderPage(pageId).collectAsState().value
                    currentBitmap?.let {
                        Image(it, contentDescription = "Image of PDF")
                    }
                }
            }
        }
    }
    /*DisposableEffect(Unit) {
        @OptIn(DelicateCoroutinesApi::class)
        onDispose {
            GlobalScope.launch { temporaryFile?.delete() }
            renderer.value?.close()
        }
    }


    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { viewSize = it }
    ) {
        val pdfReader = renderer.value
        val dpi = dpi.value
        if (pdfReader != null && dpi != null && pdfReader.pageCount > 0) {
            val lazyListState = rememberLazyListState()
            val horizontalScroll = rememberScrollState()
            LazyColumn(
                modifier = Modifier
                    .transformable(state)
                    .horizontalScroll(horizontalScroll)
                    .simpleVerticalScrollbar(lazyListState, MaterialTheme.colorScheme.primary)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(8.dp),
                state = lazyListState,
                userScrollEnabled = !isZooming,
                content = {
                    items(count = pdfReader.pageCount, key = { it }) { pageId ->
                        val cacheKey = "$pageId:${dpi}"
                        val currentPage = cache.value[cacheKey]?.second ?: run {
                            pdfReader.openPage(pageId).use { page ->
                                val bitmap = Bitmap.createBitmap(
                                    (page.width * dpi).toInt(),
                                    (page.height * dpi).toInt(),
                                    Bitmap.Config.ARGB_8888
                                )
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

        } //No Renderer yet -> Display a loading indicator
        else if (pdfReader == null) CenteredElement {
            CircularProgressIndicator(Modifier.size(32.dp))
        }
        else if (pdfReader.pageCount > 0) {
            CenteredElement {
                Text(i18nView.fileOverlayPreviewNotSupported())
            }

        }
    }*/
}

