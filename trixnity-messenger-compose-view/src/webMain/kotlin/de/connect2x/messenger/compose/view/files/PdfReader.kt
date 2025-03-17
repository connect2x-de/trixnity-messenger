package de.connect2x.messenger.compose.view.files

import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.flow.MutableStateFlow
import web.blob.Blob
import web.canvas.CanvasRenderingContext2D
import web.dom.Element
import web.dom.document
import web.html.HTMLCanvasElement
import web.url.URL


class PdfReaderWeb(blob: Blob) {
    init {
        val jsReader = getDocument(URL(URL.createObjectURL(blob)))
        jsReader.promise.then { loadedDocument ->
            pdfDocument.value = loadedDocument
            pageSize.value = loadedDocument.numPages
            viewerFrame.value = document.createElement("div").also {
                it.id = "pdf-viewer-frame"
                val body = document.getElementsByTagName("body")[0]
                body.insertBefore(it, body.firstChild)
            }
        }
    }

    val scale = mutableStateOf(1f)

    private val pdfDocument = mutableStateOf<PDFDocumentProxy?>(null)
    private val viewerFrame = mutableStateOf<Element?>(null)
    val pageSize = mutableStateOf<Int?>(null)

    private data class ViewportParams(override var scale: Float) : GetViewportParameters
    private data class RenderParams(
        override var canvasContext: CanvasRenderingContext2D,
        override var viewport: PDFPageViewport
    ) : PDFRenderParams

    fun renderPage(pageIndex: Int): MutableStateFlow<String?> {
        pageSize.value?.let { pageSize ->
            if (pageIndex <= pageSize) {
                val result = MutableStateFlow<String?>(null)
                val document = pdfDocument.value
                document?.getPage(pageIndex)?.then { page ->
                    val viewport = page.getViewport(params = ViewportParams(scale.value))
                    val canvas = getOrCreatePageCanvas(pageIndex)
                    val context = canvas.getContext(CanvasRenderingContext2D.ID)
                    canvas.height = viewport.height
                    canvas.width = viewport.width
                    context?.let {
                        page.render(RenderParams(context, viewport))
                    }
                    result.value = canvas.toDataURL()
                } ?: throw IllegalStateException("Pages with index $pageIndex doesn't exist")
                return result
            } else throw IllegalArgumentException("Page index must be smaller or equal to Page size ($pageSize)")
        } ?: throw IllegalStateException("Pages haven't been loaded yet")
    }

    private fun getOrCreatePageCanvas(pageId: Int): HTMLCanvasElement {
        return (document.getElementById("pdf-canvas-page-$pageId") ?: document.createElement("canvas")
            .also { element ->
                element.id = "pdf-canvas-page-$pageId"
                viewerFrame.value?.appendChild<Element>(element)
            }) as HTMLCanvasElement
    }
}
