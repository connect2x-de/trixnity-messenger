package de.connect2x.messenger.compose.view.room.timeline.element.message.details

import SimpleVerticalScrollbarState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.HorizontalScrollbar
import de.connect2x.messenger.compose.view.common.CenteredElement
import de.connect2x.messenger.compose.view.common.toHex
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.util.format
import io.github.oshai.kotlinlogging.KotlinLogging
import simpleVerticalScrollbar
import web.dom.Element
import web.dom.document
import kotlin.math.max
import kotlin.math.min

private val log = KotlinLogging.logger {}

/**
 * Creates a composable for web that renders the contents of
 * the PDF data provided by the `PdfDocumentViewModel`.
 * A div with the id is attached to the app's html body with the id `pdf-render-frame`
 * in which canvases are being managed which have the individual pages of the PDF being
 * drawn into and puppeteered by the LazyColumn component of this viewer.
 * After closure or dismissal that `pdf-render-frame` div is being removed automatically.
 * This component depends on the proper inclusion of the pdfjs package which
 * requires a Kotlin binding in the pdfjs.kt and for webpack to correctly move
 * the pdf.worker.mjs to the root of the runtime directory.
 * @property scale works just like for the other viewers and is handled internally.
 */
@Composable
actual fun PDFReader(
    element: RoomMessageTimelineElementViewModel.FileBased.File,
    scale: Float
) {
    val i18nView = DI.current.get<I18nView>()
    val media = element.media.collectAsState().value
    val filename = element.name
    var frameSize by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var pageSize by remember { mutableStateOf(DpSize(420.dp, 600.dp)) }
    val density = LocalDensity.current.density
    var cachedScale by remember { mutableStateOf(0f) }
    val contentPadding = DpSize(8.dp, 8.dp)
    val scrollX = remember { mutableStateOf(0f) }
    val frame = getViewerFrame(createIfMissing = true)
    if (frame != null) frameSize?.let {
        positionWebElement(
            frame, it, density, true,
            offset = Offset(scrollX.value, 0f),
        )
    }
    var renderError by remember { mutableStateOf<Throwable?>(null) }
    if (renderError != null) {
        log.error { renderError }
        CenteredElement {
            Text(i18nView.fileOverlayPreviewNotSupported())
        }
        return
    }
    var isReaderLoading by remember { mutableStateOf(false) }
    var reader by remember { mutableStateOf<PdfReader?>(null) }
    if (
        media != null && reader == null && !isReaderLoading) {
        try {
            log.debug { "loading pdf: $filename" }
            isReaderLoading = true
            val lib = try {
                // This needs to be invoked in order for lib to be resolved.
                externals.pdfjs.globalThis.pdfjsViewer
            } catch (_: Throwable) {
                js.globals.globalThis.pdfjsLib
            }
            lib.GlobalWorkerOptions.workerSrc = "./pdf.worker.mjs"
            lib.getDocument(media).promise.then { pdf ->
                val numPages: Int = pdf._pdfInfo.numPages
                log.debug { "pdf loaded of $numPages pages" }
                reader = PdfReader(pdf, numPages)
                isReaderLoading = false
                Unit
            }.catch { reason ->
                isReaderLoading = false
                renderError = if (reason is Throwable) reason else
                    Exception("failed to initialize pdf reader $reason")
                isReaderLoading = false
                Unit
            }
        } catch (e: Exception) {
            isReaderLoading = false
            renderError = Exception("failed to initialize pdf reader!", e)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            clearAllPageCanvases()
            removeViewerFrame()
        }
    }

    val scaledPageSize = DpSize(
        pageSize.width * scale - contentPadding.width * 2,
        pageSize.height * scale - contentPadding.height * 2,
    )
    val lazyListState = rememberLazyListState()
    val horizontalScroll = rememberScrollState()
    val verticalScrollbarState = remember { mutableStateOf<SimpleVerticalScrollbarState?>(null) }
    val coordinatesCache by remember {
        mutableStateOf<MutableMap<Int, LayoutCoordinates>>(mutableMapOf())
    }
    if (scale != cachedScale) {
        cachedScale = scale
        clearAllPageCanvases() // Force redraw pages.
    }
    val verticalScrollBarWidth = 8.dp
    frameSize?.let { frame ->
        getVerticalScrollBar(true)?.let {
            positionWebElement(
                it, frame, density,
                offset = Offset(
                    min(
                        scaledPageSize.width.value * density + verticalScrollBarWidth.value * density,
                        frame.size.width.toFloat() - verticalScrollBarWidth.value * density
                    ) - scrollX.value,
                    verticalScrollbarState.value?.topLeft?.y ?: 0f,
                ),
                alpha = verticalScrollbarState.value?.alpha ?: 0f,
                fixedSize = verticalScrollbarState.value?.size ?: Size.Zero,
                colorFill = MaterialTheme.colorScheme.primary.toHex(),
                zIndex = 9001,
            )
        }
    }
    fun renderPage(pageId: Int, page: dynamic) {
        coordinatesCache[pageId]?.let { coords ->
            try {
                // Skip redraws of existing pages.
                // If there's issues in the future
                // then remove the exiting page instead.
                if (getPageCanvas(pageId) != null) return

                val viewport = page.getViewport(PdfPageViewportRequest(scale))
                val canvas: dynamic = getPageCanvas(pageId, createIfMissing = true)
                val context = canvas.getContext("2d")
                canvas.height = viewport.height
                canvas.width = viewport.width
                val w: Int = viewport.width
                val h: Int = viewport.height
                pageSize = DpSize((w / density).dp, (h / density).dp)
                positionWebElement(
                    canvas, coords, density,
                    offset = Offset(contentPadding.width.value * density, 0f),
                )
                val renderContext = PdfPageRenderRequest(
                    canvasContext = context,
                    viewport = viewport,
                )
                page.render(renderContext).promise.then {
                    log.debug { "pdf page $pageId rendered" }
                }.catch { e ->
                    renderError = safeError(e, "unable to render pdf page $pageId")
                    Unit
                }
            } catch (e: Exception) {
                renderError = Exception("unable to render pdf page $pageId!", e)
            }
        }
    }

    Box(
        Modifier.fillMaxSize()
            .nestedHorizontalScroll(
                scrollX,
                frameSize?.size?.width?.toFloat() ?: 0f,
                scaledPageSize.width.value * density + contentPadding.width.value * density * 2
            )
            .onGloballyPositioned { frameSize = it }
    ) {
        reader?.let { pdfReader ->
            if (pdfReader.numPages == 0) {
                renderError = Exception("pdf has no pages")
            } else LazyColumn(
                modifier = Modifier
                    .simpleVerticalScrollbar(
                        lazyListState,
                        Color.Transparent,
                        width = verticalScrollBarWidth,
                        scrollBarState = verticalScrollbarState,
                    )
                    .horizontalScroll(horizontalScroll) // Needed to not squash pages horizontally.
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(contentPadding.height),
                contentPadding = PaddingValues(
                    horizontal = contentPadding.width,
                    vertical = contentPadding.height,
                ),
                state = lazyListState,
                content = {
                    items(count = pdfReader.numPages, key = { it }) { pageId ->
                        log.debug { "loading page $pageId:" }
                        pdfReader.pdf.getPage(pageId + 1)
                            .then { page ->
                                log.debug {
                                    "render pdf page $pageId" +
                                            " to canvas (${pageSize.width} x ${pageSize.height})" +
                                            " at scale factor: $scale"
                                }
                                renderPage(pageId, page)
                            }
                            .catch { e ->
                                renderError = safeError(e, "unable to get page $pageId")
                                Unit
                            }
                        Box(
                            modifier = Modifier
                                .onGloballyPositioned {
                                    coordinatesCache[pageId] = it
                                    getPageCanvas(pageId)?.let { canvas: dynamic ->
                                        positionWebElement(
                                            canvas, it, density,
                                            offset = Offset(contentPadding.width.value * density, 0f),
                                        )
                                    }
                                }
                                .width(scaledPageSize.width)
                                .height(scaledPageSize.height),
                        ) {
                            DisposableEffect(Unit) {
                                onDispose {
                                    log.debug { "removed pdf page item: $pageId" }
                                    removePageCanvas(pageId)
                                }
                            }
                        }
                    }
                }
            )
        } ?: Column(
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

@OptIn(ExperimentalComposeUiApi::class)
private fun Modifier.nestedHorizontalScroll(
    offsetX: MutableState<Float>,
    offsetMinX: Float,
    offsetMaxX: Float,
): Modifier = Modifier
    .onPointerEvent(PointerEventType.Scroll) {
        val event = it.changes.first()
        val scrollDelta = event.scrollDelta
        offsetX.value = max(
            -max(0f, offsetMaxX - offsetMinX),
            min(0f, offsetX.value - scrollDelta.x),
        )
        // Do not consume the event here!
    }
    .onSizeChanged {
        offsetX.value = max(
            -max(0f, offsetMaxX - offsetMinX),
            min(0f, offsetX.value),
        )
        // Do not consume the event here!
    }

private fun positionWebElement(
    element: Element,
    coordinates: LayoutCoordinates,
    density: Float,
    positionInWindow: Boolean = false,
    offset: Offset = Offset.Zero,
    fixedSize: Size? = null,
    colorFill: String = "#0000",
    alpha: Float = 1f,
    zIndex: Int = 0,
) {
    element.setAttribute(
        "style", """
        position: absolute;
        margin-top: ${
            (((if (positionInWindow) coordinates.positionInWindow()
            else coordinates.positionInParent()).y + offset.y) / density).format()
        }px;
        margin-left: ${
            (offset.x / density).format()
        }px;
        width: ${
            ((fixedSize?.width ?: (coordinates.size.width + max(0f, -offset.x)))
                    / density).format()
        }px;
        height: ${
            ((fixedSize?.height ?: coordinates.size.height.toFloat())
                    / density).format()
        }px;
        pointer-events: none;
        overflow: clip;
        opacity: ${alpha.format()};
        z-index: $zIndex;
        background: $colorFill;
        """.trimIndent()
    )
}

private fun getPageCanvas(pageId: Int, createIfMissing: Boolean = false): Element? =
    document.getElementById("pdf-canvas-page-$pageId")
        ?: if (createIfMissing) {
            document.createElement("canvas").also {
                it.setAttribute("id", "pdf-canvas-page-$pageId")
                getViewerFrame(createIfMissing)?.appendChild(it)
            }
        } else null

private fun getVerticalScrollBar(createIfMissing: Boolean = false): Element? =
    document.getElementById("pdf-viewer-vertical-scroll-bar")
        ?: if (createIfMissing) {
            document.createElement("div").also {
                it.setAttribute("id", "pdf-viewer-vertical-scroll-bar")
                getViewerFrame(createIfMissing)?.appendChild(it)
            }
        } else null

private fun removePageCanvas(pageId: Int) {
    getPageCanvas(pageId)?.let { canvas: dynamic ->
        getViewerFrame()?.removeChild(canvas)
    }
}

private fun getViewerFrame(createIfMissing: Boolean = false): Element? =
    document.getElementById("pdf-render-frame")
        ?: if (createIfMissing) document.createElement("div").also {
            it.setAttribute("id", "pdf-render-frame")
            val body = document.getElementsByTagName("body")[0]
            body?.insertBefore(it, body.firstChild)
        } else null

private fun clearAllPageCanvases() {
    getViewerFrame()?.innerHTML = ""
}

private fun removeViewerFrame() {
    getViewerFrame()?.let {
        document.getElementsByTagName("body")[0]?.removeChild(it)
    }
}

private fun safeError(e: dynamic, message: String? = null): Throwable =
    if (e is Throwable) (e as Throwable).let { if (message != null) Exception(message, it) else it }
    else Exception((if (message != null) "$message: " else "") + e.toString())

private data class PdfReader(
    val pdf: dynamic,
    val numPages: Int,
)

@OptIn(ExperimentalJsExport::class)
@JsExport
data class PdfPageViewportRequest(
    val scale: Float,
)

@OptIn(ExperimentalJsExport::class)
@JsExport
data class PdfPageRenderRequest(
    val canvasContext: dynamic,
    val viewport: dynamic,
)
