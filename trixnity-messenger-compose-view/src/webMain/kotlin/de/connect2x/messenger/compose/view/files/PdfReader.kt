package de.connect2x.messenger.compose.view.files

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import io.github.oshai.kotlinlogging.KotlinLogging
import js.date.Date
import js.typedarrays.Uint8Array
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import web.blob.Blob
import web.canvas.CanvasRenderingContext2D
import web.dom.document
import web.html.HTMLCanvasElement
import web.url.URL
import kotlin.math.max
import kotlin.math.min

private val log = KotlinLogging.logger { }

class PdfReaderWeb(blob: Blob) {
    init {
        val jsReader = getDocument(URL(URL.createObjectURL(blob)))
        jsReader.promise.then { loadedDocument ->
            pdfDocument.value = loadedDocument
            pageSize.value = loadedDocument.numPages
            loadedDocument.getPage(1).then { documentWidth.value = it.getViewport(ViewportParams(1f)).width }
        }
    }


    val documentWidth = mutableStateOf<Int?>(null)
    val cache = mutableMapOf<String, Triple<Long, MutableStateFlow<ImageBitmap?>, Int>>()

    private val pdfDocument = mutableStateOf<PDFDocumentProxy?>(null)
    private val dom = document
    val pageSize = mutableStateOf<Int?>(null)

    private data class ViewportParams(override var scale: Float) : GetViewportParameters
    private data class RenderParams(
        override var canvasContext: CanvasRenderingContext2D,
        override var viewport: PDFPageViewport
    ) : PDFRenderParams

    fun renderPage(pageIndex: Int, urlFlow: MutableStateFlow<ImageBitmap?>, scale: Float) {
        pageSize.value?.let { pageSize ->
            if (pageIndex <= pageSize) {
                val canvas = getOrCreatePageCanvas(pageIndex)
                val document = pdfDocument.value
                document?.getPage(pageIndex)?.then { page ->
                    val scaledViewport = page.getViewport(params = ViewportParams(scale = scale))
                    log.debug {
                        "render pdf page $pageIndex " +
                                "to viewport (${scaledViewport.width}x${scaledViewport.height}) " +
                                "at scale factor: $scale " +
                                "with ${cache.size} pages already cached"
                    }
                    canvas.height = scaledViewport.height
                    canvas.width = scaledViewport.width
                    val context = canvas.getContext(CanvasRenderingContext2D.ID)
                    context?.let {
                        page.render(RenderParams(context, scaledViewport)).promise.then {
                            canvas.toBlob(callback = { blob ->
                                blob?.let {
                                    blob.arrayBufferAsync().then { buffer ->
                                        urlFlow.value = Uint8Array(buffer).toByteArray().toImageBitmap()
                                        dom.getElementById("pdf-canvas-page-$pageIndex")?.remove()
                                    }
                                }
                            })
                        }
                    }
                } ?: throw IllegalStateException("Page with index $pageIndex doesn't exist")
            } else throw IllegalArgumentException("Page index must be smaller or equal to Page size ($pageSize)")
        } ?: throw IllegalStateException("Pages haven't been loaded yet")
    }

    private fun getOrCreatePageCanvas(pageId: Int): HTMLCanvasElement {
        return (dom.getElementById("pdf-canvas-page-$pageId") ?: dom.createElement("canvas")
            .also { element ->
                element.id = "pdf-canvas-page-$pageId"
            }) as HTMLCanvasElement
    }

    fun getOrRenderPage(pageId: Int, dpi: Float, scale: Float): StateFlow<ImageBitmap?> {
        val oneBasedId = pageId + 1
        val pageScale = dpi / 72f
        val cacheKey = "$oneBasedId"
        val element = cache[cacheKey]
        val pageCacheSize = max(2f, min(16f, 8f / scale)).toInt()

        return if (element?.third == pageScale.toInt()) {
            element.second
        } else element?.second?.also {
            renderPage(oneBasedId, it, pageScale)
            removeOldElements(pageCacheSize)
        }
            ?: MutableStateFlow<ImageBitmap?>(null).also {
                cache[cacheKey] = Triple(Date.now().toLong(), it, pageScale.toInt())
                renderPage(oneBasedId, it, pageScale)
                removeOldElements(pageCacheSize)
            }
    }

    private fun removeOldElements(pageCacheSize: Int) {
        cache.toList().sortedBy { it.second.first }
            .subList(0, 0.coerceAtLeast(cache.size - pageCacheSize))
            .forEach { cache.remove(it.first) }
    }

    fun clearCache() {
        cache.clear()
    }
}
