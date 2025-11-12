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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.then
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.HorizontalScrollbar
import de.connect2x.messenger.compose.view.VerticalScrollbar
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.CenteredElement
import de.connect2x.messenger.compose.view.common.DownloadProgress
import de.connect2x.messenger.compose.view.common.LoadingSpinner
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ThemedProgressIndicator
import de.connect2x.messenger.compose.view.theme.messengerDpConstants
import de.connect2x.messenger.compose.view.theme.messengerIcons
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.media.PlatformMedia
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KClass
import kotlin.time.Clock

private val log = KotlinLogging.logger {}

interface PdfTimelineElementDetailsView : TimelineElementDetailsView<RoomMessageTimelineElementViewModel.FileBased.File>

class PdfTimelineElementDetailsViewImpl : PdfTimelineElementDetailsView {
    override val supports: KClass<RoomMessageTimelineElementViewModel.FileBased.File> =
        RoomMessageTimelineElementViewModel.FileBased.File::class

    override fun supportsMimeType(mimeType: ContentType): Boolean {
        return ContentType.Application.Pdf.match(mimeType)
    }


    private fun removeOldElements(pageCacheSize: Int, lazyListState: LazyListState) {
        val visibleItems = lazyListState.layoutInfo.visibleItemsInfo.map { itemInfo -> itemInfo.index }
        val creationOrderedList = cache.toList().sortedBy { it.second.value?.creationTime }
            .filter { !visibleItems.contains(it.first) && it.second.value != null }
        creationOrderedList.subList(0, (cache.size - pageCacheSize).coerceIn(0, creationOrderedList.size))
            .forEach {
                cache.remove(it.first)
            }
    }

    //The pdf page cache, consisting of the page number as the key and the time it was loaded, the image itself and its DPI as the value
    private val cache =
        mutableStateMapOf<Int, MutableStateFlow<PDFCacheEntry?>>()

    private fun getCacheElement(cacheKey: Int, scope: CoroutineScope): StateFlow<PDFCacheEntry?> {
        return cache[cacheKey] ?: run {
            return MutableStateFlow<PDFCacheEntry?>(null).also {
                cache[cacheKey] = it
                scope.launch {
                    queue.emit(cacheKey)
                }
            }
        }
    }

    val queue = MutableSharedFlow<Int>()

    private suspend fun loadImageWithDpi(
        reader: PDFReader,
        pageId: Int,
        dpi: Float,
        pageCacheSize: Int,
        lazyListState: LazyListState
    ) {
        val element = cache[pageId]
        if (element?.value?.dpi != dpi.toInt()) {
            removeOldElements(pageCacheSize, lazyListState)
            element?.value =
                PDFCacheEntry(Clock.System.now().toEpochMilliseconds(), reader.getPage(pageId, dpi), dpi.toInt())
        }
    }

    private fun calcSizeOnZoom(constraints: Constraints, zoom: Float, density: Float): DpSize {
        return DpSize(
            (constraints.maxWidth * zoom / density).dp,
            (constraints.maxHeight * zoom / density).dp
        )
    }

    private fun Size.divideComponents(other: Size): Size {
        return Size(this.width / other.width, this.height / other.height)
    }

    private fun Size.multiplyComponents(other: Size): Size {
        return Size(this.width * other.width, this.height * other.height)
    }

    private fun Size.subtractComponents(other: Size): Size {
        return Size(this.width - other.width, this.height - other.height)
    }

    private fun getNewZoomOffsetDelta(
        viewportSize: Size,
        oldZoomFactor: Float,
        newZoomFactor: Float
    ): Size {
        val oldMaxSize = viewportSize * oldZoomFactor
        val oldCenter = viewportSize / 2f
        val newSize = viewportSize * newZoomFactor
        val oldFraction = oldCenter.divideComponents(oldMaxSize)
        val newCenter = newSize.multiplyComponents(oldFraction)
        val newOffset = newCenter.subtractComponents(viewportSize / 2f)
        return newOffset
    }

    @Composable
    override fun create(
        element: RoomMessageTimelineElementViewModel.FileBased.File,
        onSave: () -> Unit,
        onClose: () -> Unit,
    ) {
        val minZoom = 0.5f
        val maxZoom = 4f
        val media = element.loadMediaResultPlatformMedia.collectAsState().value
        val progress = element.loadMediaProgress.collectAsState().value
        val (error, setError) = remember { mutableStateOf<String?>(null) }
        val zoom = remember { mutableStateOf(1.0f) }
        val canZoom = remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        val lazyListState = rememberLazyListState()
        val horizontalScroll = rememberScrollState()
        val reader = remember { mutableStateOf<PDFReader?>(null) }
        val currentSize = remember { mutableStateOf(DpSize.Zero) }
        val scrollRequest = remember { mutableStateOf<Size?>(null) }
        LaunchedEffect(scrollRequest.value) {
            val currentRequest = scrollRequest.value
            if (currentRequest != null) {
                horizontalScroll.scrollBy(currentRequest.width)
                lazyListState.scrollBy(currentRequest.height)
                scrollRequest.value = null
            }
        }

        //Control all changes to zoom/offset
        val state = rememberTransformableState { zoomChange, offsetChange, _ ->
            scope.launch {
                if (zoomChange != 1f) {
                    val oldZoom = zoom.value
                    zoom.value = (zoom.value * zoomChange).coerceIn(minZoom, maxZoom)
                    val newOffset = getNewZoomOffsetDelta(
                        lazyListState.layoutInfo.viewportSize.toSize(),
                        oldZoom,
                        zoom.value
                    )
                    //Necessary to assure the new size has been measured, otherwise the scrolling won't work
                    scrollRequest.value = newOffset
                } else {
                    val offset = offsetChange * zoom.value
                    horizontalScroll.scrollBy(-offset.x)
                    lazyListState.scrollBy(-offset.y)
                }
            }
        }
        val i18n = DI.get<I18nView>()
        val dpi = remember { mutableStateOf<Float?>(null) }
        val pageCacheSize = remember { mutableStateOf(max(2f, min(16f, 8f / zoom.value)).toInt()) }

        LaunchedEffect(media) {
            if (media == null) { // if the pdf is opened a second time there's no need to re-download it
                element.loadMedia()
            }
        }
        LaunchedEffect(element.loadMediaError) {
            element.loadMediaError.collect { if (it != null) setError(i18n.fileCouldNotBeLoaded()) }
        }

        FileBasedDetailsDialog(
            element,
            onSave,
            onClose,
            additions = {
                PageIndicator(lazyListState, reader.value?.numOfPages?.value ?: 0)
                ZoomButtons(state, scope)
            }) {
            val focusRequester = remember { FocusRequester() }
            BoxWithConstraints(
                Modifier
                    .background(color = Color.Black)
                    .fillMaxSize()
                    .focusRequester(focusRequester)
                    .focusable()
                    .onKeyEvent { keyEvent ->
                        canZoom.value = keyEvent.isCtrlPressed || keyEvent.isMetaPressed
                        false
                    }
                    .zoomModifier(focusRequester, canZoom, state, scope),
                contentAlignment = Alignment.TopCenter
            ) {
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
                        LaunchedEffect(
                            reader.value?.documentWidth?.value,
                            constraints.maxWidth,
                            constraints.maxHeight,
                            zoom.value,
                        ) {
                            reader.value?.documentWidth?.value?.let {
                                val maxDpi = 1f / it.toFloat() * 64f * 3600f
                                val dpiTarget = density * zoom.value
                                dpi.value = (dpiTarget * it).coerceAtMost(maxDpi)
                                currentSize.value = calcSizeOnZoom(constraints, zoom.value, density)
                            }
                        }
                        val numOfPages = reader.value?.numOfPages?.value
                        val currentReader = reader.value
                        val dpi = dpi.value

                        if (currentReader != null && numOfPages != null && dpi != null) {
                            LaunchedEffect(Unit) {
                                queue.collect {
                                    pageCacheSize.value = max(3f, min(16f, 8f / zoom.value)).toInt()
                                    loadImageWithDpi(
                                        currentReader,
                                        it,
                                        dpi,
                                        pageCacheSize.value,
                                        lazyListState
                                    )
                                }
                            }
                            LazyColumn(
                                modifier = Modifier
                                    .horizontalScroll(state = horizontalScroll, enabled = canZoom.value.not())
                                    .fillMaxSize()
                                    .transformable(state),
                                verticalArrangement = Arrangement.spacedBy(MaterialTheme.messengerDpConstants.small),
                                contentPadding = PaddingValues(horizontal = MaterialTheme.messengerDpConstants.middle),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                state = lazyListState,
                                userScrollEnabled = canZoom.value.not(),
                                content = {
                                    items(count = numOfPages, key = { it }) { pageId ->
                                        val image = getCacheElement(pageId, scope).collectAsState().value?.page
                                        LaunchedEffect(dpi) {
                                            delay(200)
                                            queue.emit(pageId)
                                        }
                                        if (image != null) {
                                            Image(
                                                bitmap = image,
                                                contentDescription = i18n.fileOverlayPdfPageDescriptor(pageId),
                                                modifier = Modifier
                                                    .background(color = Color.White) // Avoid performance drops on transparent images.
                                                    .width(currentSize.value.width),
                                                contentScale = ContentScale.FillWidth,
                                            )
                                        } else {
                                            Box(
                                                Modifier.size(currentSize.value),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                LoadingSpinner()
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

interface PDFReader {
    suspend fun getPage(pageId: Int, dpi: Float): ImageBitmap?
    fun onDispose()
    val numOfPages: MutableState<Int?>
    val documentWidth: MutableState<Int?>
}

data class PDFCacheEntry(val creationTime: Long, val page: ImageBitmap?, val dpi: Int)

expect suspend fun getPlatformPDFReader(
    media: PlatformMedia,
    onError: (String?) -> Unit,
): PDFReader

private data class PageNumber(val pageIndex: Int, val scroll: Boolean, val updateIndexViaList: Boolean)

@Composable
private fun PageIndicator(lazyListState: LazyListState, pageCount: Int) {
    val measurer = rememberTextMeasurer()
    val textStyle = MaterialTheme.typography.headlineSmall.copy(color = Color.LightGray, textAlign = TextAlign.Right)
    val pageText = rememberTextFieldState()
    val currentIndex = remember { mutableStateOf(PageNumber(0, scroll = false, updateIndexViaList = true)) }
    val density = LocalDensity.current
    //Set the width to the digit count of the maximum page number + 1
    val inputFieldWidth = remember(pageCount) {
        if (pageCount != 0) {
            measurer.measure(
                text = "0".repeat(log10(pageCount.toFloat()).toInt() + 2),
                style = textStyle,
                density = density
            ).size.width.dp / density.density
        } else measurer.measure(text = "00", style = textStyle, density = density).size.width.dp / density.density
    }
    //Update the page numbers when new pages are scrolled into view
    LaunchedEffect(Unit) {
        snapshotFlow { lazyListState.layoutInfo.visibleItemsInfo.map { it.index } }.distinctUntilChanged { old, new -> old.firstOrNull() == new.firstOrNull() }
            .collect { visibleItems ->
                if (currentIndex.value.updateIndexViaList) {
                    visibleItems.firstOrNull()?.let {
                        currentIndex.value = PageNumber(
                            it,
                            scroll = false,
                            updateIndexViaList = true
                        )
                    }
                }
                else currentIndex.value = currentIndex.value.copy(updateIndexViaList = true)
            }
    }

    LaunchedEffect(currentIndex.value.pageIndex) {
        if (currentIndex.value.scroll) {
            lazyListState.scrollToItem(currentIndex.value.pageIndex)
        }
        pageText.edit {
            replace(0, length, currentIndex.value.pageIndex.inc().toString())
        }
    }

    Surface(shape = MaterialTheme.shapes.medium, color = Color.DarkGray) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                enabled = currentIndex.value.pageIndex > 0,
                onClick = {
                    if (currentIndex.value.pageIndex > 0) {
                        currentIndex.value = PageNumber(currentIndex.value.pageIndex.dec(),
                            scroll = true,
                            updateIndexViaList = true
                        )
                    }
                },
                modifier = Modifier.buttonPointerModifier(),
            ) {
                Icon(Icons.Default.Remove, "To previous page")
            }
            BasicTextField(
                state = pageText,
                lineLimits = TextFieldLineLimits.SingleLine,
                textStyle = textStyle,
                cursorBrush = SolidColor(Color.LightGray),
                inputTransformation = InputTransformation.then {
                    if (toString() != "") {
                        val inputNumber = this.toString().toIntOrNull()?.coerceIn(1, pageCount)
                        if (inputNumber != null) {
                            //Don't scroll to the start of the current page when selecting the TextField
                            currentIndex.value =
                                PageNumber(
                                    inputNumber.dec(),
                                    inputNumber.dec() != currentIndex.value.pageIndex,
                                    lazyListState.layoutInfo.visibleItemsInfo.map { itemInfo -> itemInfo.index }
                                        .contains(inputNumber.dec())
                                )
                            replace(0, length, currentIndex.value.pageIndex.inc().toString())
                        } else {
                            revertAllChanges()
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                decorator = { inputField ->
                    Row(Modifier.padding(MaterialTheme.messengerDpConstants.verySmall)) {
                        Surface(
                            Modifier.width(inputFieldWidth),
                            shape = MaterialTheme.shapes.extraSmall,
                            color = Color.Black
                        ) {
                            inputField()
                        }
                        Text(" / $pageCount", style = textStyle)
                    }
                }
            )
            IconButton(
                enabled = currentIndex.value.pageIndex < pageCount - 1,
                onClick = {
                    if (currentIndex.value.pageIndex < pageCount - 1) {
                        currentIndex.value = PageNumber(currentIndex.value.pageIndex.inc(),
                            scroll = true,
                            updateIndexViaList = true
                        )
                    }
                },
                modifier = Modifier.buttonPointerModifier(),
            ) {
                Icon(Icons.Default.Add, "To next page")
            }
        }
    }
}
