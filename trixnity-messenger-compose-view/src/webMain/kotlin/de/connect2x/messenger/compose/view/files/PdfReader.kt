package de.connect2x.messenger.compose.view.files

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import js.date.Date
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import web.blob.Blob
import web.canvas.CanvasRenderingContext2D
import web.dom.document
import web.html.HTMLCanvasElement
import web.url.URL
import kotlin.math.max
import kotlin.math.min


class PdfReaderWeb(blob: Blob) {
    init {
        val jsReader = getDocument(URL(URL.createObjectURL(blob)))
        jsReader.promise.then { loadedDocument ->
            pdfDocument.value = loadedDocument
            pageSize.value = loadedDocument.numPages
        }
    }

    private val pageCacheSize = max(2f, min(16f, 8f / 1f)).toInt()

    val scale = mutableStateOf(1f)
    val cache = mutableMapOf<String, Pair<Long, MutableStateFlow<ImageBitmap?>>>()

    private val pdfDocument = mutableStateOf<PDFDocumentProxy?>(null)
    private val dom = document
    val pageSize = mutableStateOf<Int?>(null)

    private data class ViewportParams(override var scale: Float) : GetViewportParameters
    private data class RenderParams(
        override var canvasContext: CanvasRenderingContext2D,
        override var viewport: PDFPageViewport
    ) : PDFRenderParams

    fun renderPage(pageIndex: Int, urlFlow: MutableStateFlow<ImageBitmap?>) {
        pageSize.value?.let { pageSize ->
            if (pageIndex <= pageSize) {
                val canvas = getOrCreatePageCanvas(pageIndex)
                val document = pdfDocument.value
                document?.getPage(pageIndex)?.then { page ->
                    val viewport = page.getViewport(params = ViewportParams(scale.value))
                    val context = canvas.getContext(CanvasRenderingContext2D.ID)
                    canvas.height = viewport.height
                    canvas.width = viewport.width
                    context?.let {
                        page.render(RenderParams(context, viewport)).promise.then {
                            canvas.toBlob(callback = { blob ->
                                blob?.let {
                                    blob.bytesAsync().then {
                                        urlFlow.value = it.toByteArray().toImageBitmap()
                                        println("Created bitmap for $pageIndex")
                                    }
                                }
                            })
                        }
                    }
                } ?: throw IllegalStateException("Pages with index $pageIndex doesn't exist")
            } else throw IllegalArgumentException("Page index must be smaller or equal to Page size ($pageSize)")
        } ?: throw IllegalStateException("Pages haven't been loaded yet")
    }

    private fun getOrCreatePageCanvas(pageId: Int): HTMLCanvasElement {
        return (dom.getElementById("pdf-canvas-page-$pageId") ?: dom.createElement("canvas")
            .also { element ->
                element.id = "pdf-canvas-page-$pageId"
            }) as HTMLCanvasElement
    }

    fun getOrRenderPage(pageId: Int): StateFlow<ImageBitmap?> {
        println("Starting render of $pageId")
        val oneBasedId = pageId + 1
        val cacheKey = "$oneBasedId"
        return cache[cacheKey]?.second ?: run {
            val urlFlow = MutableStateFlow<ImageBitmap?>(null)
            cache[cacheKey] = Pair(Date.now().toLong(), urlFlow)
            urlFlow.also {
                renderPage(oneBasedId, urlFlow)
                cache.toList().sortedBy { it.second.first }
                    .subList(0, 0.coerceAtLeast(cache.size - pageCacheSize))
                    .forEach { cache.remove(it.first) }
            }
        }
    }
}
