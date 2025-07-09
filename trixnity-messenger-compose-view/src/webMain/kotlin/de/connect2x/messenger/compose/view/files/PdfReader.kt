package de.connect2x.messenger.compose.view.files

import androidx.compose.ui.graphics.ImageBitmap
import pdfjs.GetViewportParameters
import pdfjs.PDFDocumentProxy
import pdfjs.RenderParameters
import pdfjs.getDocument
import io.github.oshai.kotlinlogging.KotlinLogging
import js.typedarrays.Uint8Array
import js.typedarrays.toByteArray
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import web.blob.Blob
import web.canvas.CanvasRenderingContext2D
import web.dom.ElementId
import web.dom.document
import web.html.HTMLCanvasElement
import web.url.URL

private val log = KotlinLogging.logger { }

class PdfReaderWeb(blob: Blob) {
    init {
        val jsReader = getDocument(URL(URL.createObjectURL(blob)))
        jsReader.promise.then { loadedDocument ->
            pdfDocument.value = loadedDocument
            pageSize.value = loadedDocument.numPages.toInt()
            loadedDocument.getPageAsync(1)
                .then { documentWidth.value = it.getViewport(GetViewportParameters(1f)).width.toInt() }
        }
    }

    val documentWidth = MutableStateFlow<Int?>(null)

    private val pdfDocument = MutableStateFlow<PDFDocumentProxy?>(null)
    private val dom = document
    val pageSize = MutableStateFlow<Int?>(null)

    suspend fun renderPage(pageIndex: Int, bitmapFlow: MutableStateFlow<ImageBitmap?>, dpi: Float) {
        val scale = dpi.div(72f)
        val pageSize = pageSize.filterNotNull().first()
        if (pageIndex <= pageSize) {
            val canvas = getOrCreatePageCanvas(pageIndex)
            val document = pdfDocument.filterNotNull().first()
            document.getPageAsync(pageIndex).then { page ->
                val scaledViewport = page.getViewport(GetViewportParameters(scale = scale))
                log.debug {
                    "render pdf page $pageIndex " +
                            "to viewport (${scaledViewport.width}x${scaledViewport.height}) " +
                            "at scale factor: $scale "
                }
                canvas.height = scaledViewport.height.toInt()
                canvas.width = scaledViewport.width.toInt()
                val context = canvas.getContext(CanvasRenderingContext2D.ID)
                context?.let {
                    page.render(RenderParameters(canvasContext = context, viewport = scaledViewport)).promise.then {
                        canvas.toBlob(callback = { blob ->
                            blob?.let {
                                blob.arrayBufferAsync().then { buffer ->
                                    bitmapFlow.value = Uint8Array(buffer).toByteArray().toImageBitmap()
                                    dom.getElementById(ElementId("pdf-canvas-page-$pageIndex"))?.remove()
                                }
                            }
                        })
                    }
                }
            }
        } else throw IllegalArgumentException("Page index must be smaller or equal to Page size ($pageSize)")
    }

    private fun getOrCreatePageCanvas(pageId: Int): HTMLCanvasElement {
        return (dom.getElementById(ElementId("pdf-canvas-page-$pageId")) ?: dom.createElement("canvas")
            .apply {
                id = ElementId("pdf-canvas-page-$pageId")
            }) as HTMLCanvasElement
    }
}
