package de.connect2x.messenger.compose.view.room.timeline.element.details

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.HorizontalScrollbar
import de.connect2x.messenger.compose.view.VerticalScrollbar
import de.connect2x.messenger.compose.view.common.CenteredElement
import de.connect2x.messenger.compose.view.common.DownloadProgress
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ThemedProgressIndicator
import de.connect2x.messenger.compose.view.theme.messengerDpConstants
import de.connect2x.messenger.compose.view.theme.messengerIcons
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import net.folivo.trixnity.client.media.PlatformMedia
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KClass

private val log = KotlinLogging.logger {}

class PdfTimelineElementDetailsView : TimelineElementDetailsView<RoomMessageTimelineElementViewModel.FileBased.File> {
    override val supports: KClass<RoomMessageTimelineElementViewModel.FileBased.File> =
        RoomMessageTimelineElementViewModel.FileBased.File::class

    override fun supportsMimeType(mimeType: ContentType): Boolean {
        return ContentType.Application.Pdf.match(mimeType)
    }


    private fun removeOldElements(pageCacheSize: Int, lazyListState: LazyListState) {
        val visibleItems = lazyListState.layoutInfo.visibleItemsInfo.map { itemInfo -> itemInfo.index }
        cache.toList().sortedBy { it.second.value?.first }
            .subList(0, 0.coerceAtLeast(cache.size - pageCacheSize)).filter { !visibleItems.contains(it.first) }
            .forEach {
                cache.remove(it.first)
            }
    }

    //The pdf page cache, consisting of the page number as the key and the time it was loaded, the image itself and its DPI as the value
    private val cache =
        mutableStateMapOf<Int, MutableStateFlow<Triple<Long, ImageBitmap?, Int>?>>()

    private val mutex = Mutex()

    private fun getCacheElement(cacheKey: Int): StateFlow<Triple<Long, ImageBitmap?, Int>?> {
        return cache[cacheKey] ?: run {
            return MutableStateFlow<Triple<Long, ImageBitmap?, Int>?>(null).also { cache[cacheKey] = it }
        }
    }


    private suspend fun loadImageWithDpi(
        reader: PDFReader,
        pageId: Int,
        dpi: Float,
        pageCacheSize: Int,
        lazyListState: LazyListState
    ) = mutex.withLock {
        val element = cache[pageId]
        if (element?.value?.third != dpi.toInt()) {
            removeOldElements(pageCacheSize, lazyListState)
            element?.value = Triple(Clock.System.now().toEpochMilliseconds(), reader.getPage(pageId, dpi), dpi.toInt())
        }
    }


    @Composable
    override fun create(
        element: RoomMessageTimelineElementViewModel.FileBased.File,
        onSave: () -> Unit,
        onClose: () -> Unit,
    ) {
        val minZoom = 0.5f
        val maxZoom = 4f
        val media = element.downloadMediaResult.collectAsState().value
        val progress = element.downloadMediaProgress.collectAsState().value
        val (error, setError) = remember { mutableStateOf<String?>(null) }
        val zoom = remember { mutableStateOf(1.0f) }
        val offset = remember { mutableStateOf(Offset.Zero) }
        val canZoom = remember { mutableStateOf(false) }

        val state = rememberTransformableState { zoomChange, offsetChange, _ ->
            zoom.value = (zoom.value * zoomChange).coerceIn(minZoom, maxZoom)
            offset.value = offset.value + offsetChange.times(zoom.value)
        }
        val viewSize = remember { MutableStateFlow(IntSize.Zero) }
        val i18n = DI.get<I18nView>()
        val dpi = remember { mutableStateOf<Float?>(null) }
        val pageCacheSize = remember { mutableStateOf(max(2f, min(16f, 8f / zoom.value)).toInt()) }

        LaunchedEffect(Unit) {
            element.downloadMedia()
        }
        LaunchedEffect(element.downloadMediaError) {
            element.downloadMediaError.collect { if (it != null) setError(i18n.fileCouldNotBeLoaded()) }
        }
        FileBasedDetailsDialog(
            element,
            onSave,
            onClose,
            additions = { ZoomButtons(zoom, minScale = minZoom, maxScale = maxZoom) }) {
            val focusRequester = remember { FocusRequester() }
            Column {
                Box(
                    Modifier
                        .background(color = Color.Black)
                        .fillMaxSize()
                        .focusRequester(focusRequester)
                        .focusable()
                        .onKeyEvent { keyEvent ->
                            canZoom.value = keyEvent.isCtrlPressed || keyEvent.isMetaPressed
                            true
                        }
                        .zoomModifier(focusRequester, canZoom, zoom, minZoom, maxZoom),
                ) {
                    log.debug { "error: $error" }
                    when {
                        error != null -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize().padding(32.dp),
                            ) {
                                Icon(
                                    MaterialTheme.messengerIcons.typeFile,
                                    i18n.commonFile(),
                                    Modifier.size(96.dp).align(Alignment.CenterHorizontally),
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(error, color = Color.White)
                            }
                        }

                        progress != null && media == null -> {
                            DownloadProgress(progress, element::cancelDownloadMedia)
                        }

                        media != null -> {
                            val density = LocalDensity.current.density
                            val reader = remember { mutableStateOf<PDFReader?>(null) }
                            LaunchedEffect(Unit) {
                                reader.value =
                                    getPlatformPDFReader(media) { setError(i18n.fileCouldNotBeLoaded()) }
                            }
                            DisposableEffect(Unit) {
                                onDispose {
                                    reader.value?.onDispose()
                                    cache.clear()
                                }
                            }
                            val lazyListState = rememberLazyListState()
                            val horizontalScroll = rememberScrollState()

                            LaunchedEffect(offset.value) {
                                lazyListState.scrollBy(-offset.value.y)
                                horizontalScroll.scrollBy(-offset.value.x)
                                offset.value = Offset.Zero
                            }
                            LaunchedEffect(
                                reader.value?.documentWidth?.value,
                                viewSize.value,
                                zoom.value,
                            ) {
                                reader.value?.documentWidth?.value?.let {
                                    val maxDpi = 1f / it.toFloat() * 64f * 3600f
                                    val dpiTarget = density * zoom.value
                                    dpi.value = (dpiTarget * it).coerceAtMost(maxDpi)
                                }
                            }

                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .onSizeChanged { viewSize.value = it },
                                contentAlignment = Alignment.Center,
                            ) {
                                val numOfPages = reader.value?.numOfPages?.value
                                val reader = reader.value
                                val dpi = dpi.value
                                if (reader != null && numOfPages != null && dpi != null) {
                                    LazyColumn(
                                        modifier = Modifier
                                            .horizontalScroll(state = horizontalScroll, enabled = canZoom.value.not())
                                            .fillMaxSize()
                                            .transformable(state),
                                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.messengerDpConstants.small),
                                        contentPadding = PaddingValues(horizontal = MaterialTheme.messengerDpConstants.middle),
                                        state = lazyListState,
                                        userScrollEnabled = canZoom.value.not(),
                                        content = {
                                            items(count = numOfPages, key = { it }) { pageId ->
                                                val image = getCacheElement(pageId).collectAsState().value?.second
                                                LaunchedEffect(dpi) {
                                                    pageCacheSize.value = max(2f, min(16f, 8f / zoom.value)).toInt()
                                                    loadImageWithDpi(
                                                        reader,
                                                        pageId,
                                                        dpi,
                                                        pageCacheSize.value,
                                                        lazyListState
                                                    )
                                                }

                                                if (image != null) {
                                                    Image(
                                                        bitmap = image,
                                                        contentDescription = i18n.fileOverlayPdfPageDescriptor(pageId),
                                                        modifier = Modifier
                                                            .background(color = Color.White) // Avoid performance drops on transparent images.
                                                            .width(viewSize.value.width.dp / density * zoom.value - MaterialTheme.messengerDpConstants.middle * 2),
                                                        contentScale = ContentScale.FillWidth,
                                                    )
                                                } else {
                                                    Box(
                                                        Modifier.height(viewSize.value.height.dp / density * zoom.value - MaterialTheme.messengerDpConstants.middle * 2)
                                                            .width(viewSize.value.width.dp / density * zoom.value - MaterialTheme.messengerDpConstants.middle * 2),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        CircularProgressIndicator()
                                                    }
                                                }
                                            }
                                        }
                                    )
                                } else CenteredElement {
                                    ThemedProgressIndicator(
                                        style = MaterialTheme.components.circularProgressIndicator
                                    )
                                }
                                HorizontalScrollbar(
                                    Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                                    horizontalScroll,
                                )
                                VerticalScrollbar(
                                    Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                                    lazyListState,
                                    false
                                )
                            }
                        }

                        else -> {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                ThemedProgressIndicator(style = MaterialTheme.components.circularProgressIndicator)
                            }
                        }
                    }
                }
            }
        }
    }
}

interface PDFReader {
    suspend fun getPage(pageId: Int, dpi: Float): ImageBitmap?
    fun onDispose()
    val numOfPages: MutableState<Int?>
    val documentWidth: MutableState<Int?>
}

expect suspend fun getPlatformPDFReader(
    media: PlatformMedia,
    onError: (String?) -> Unit,
): PDFReader
