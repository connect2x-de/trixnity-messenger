@file:OptIn(ExperimentalWasmJsInterop::class)

package de.connect2x.trixnity.messenger.compose.view.files

import androidx.compose.ui.graphics.ImageBitmap
import de.connect2x.lognity.api.logger.Logger
import js.promise.await
import js.typedarrays.Uint8Array
import js.typedarrays.toByteArray
import pdfjs.GetViewportParameters
import pdfjs.PDFDocumentProxy
import pdfjs.RenderParameters
import pdfjs.getDocument
import web.blob.Blob
import web.canvas.CanvasRenderingContext2D
import web.canvas.ID
import web.dom.ElementId
import web.dom.document
import web.html.HTMLCanvasElement
import web.html.toBlob
import web.url.URL

private val log: Logger = Logger("de.connect2x.trixnity.messenger.compose.view.files.PdfReaderKt")

suspend fun PdfReaderWeb(blob: Blob): PdfReaderWeb {
    val doc = getDocument(URL(URL.createObjectURL(blob))).promise.await()
    val documentWidth = doc.getPage(1.toJsNumber()).await().getViewport(GetViewportParameters(1.toJsNumber())).width.toInt()
    return PdfReaderWeb(doc, documentWidth)
}

class PdfReaderWeb internal constructor(
    val pdfDocument: PDFDocumentProxy,
    val documentWidth: Int,
    val pageSize: Int = pdfDocument.numPages.toInt(),
) {
    suspend fun renderPage(pageIndex: Int, dpi: Float): ImageBitmap? {
        if (pageIndex > pageSize) throw IllegalArgumentException("Page index must be smaller or equal to Page size ($pageSize)")
        val scale = dpi.div(72f)

        val page = pdfDocument.getPage(pageIndex.toJsNumber()).await()
        val scaledViewport = page.getViewport(GetViewportParameters(scale = scale.toDouble().toJsNumber()))
        log.debug {
            "render pdf page $pageIndex " +
                    "to viewport (${scaledViewport.width}x${scaledViewport.height}) " +
                    "at scale factor: $scale "
        }

        val canvas = getOrCreatePageCanvas(pageIndex)
        canvas.height = scaledViewport.height.toInt()
        canvas.width = scaledViewport.width.toInt()

        val context = canvas.getContext(CanvasRenderingContext2D.ID) ?: return null
        page.render(RenderParameters(canvasContext = context, viewport = scaledViewport)).promise.await()

        val buffer = canvas.toBlob()?.arrayBufferAsync()?.await() ?: return null
        document.getElementById(ElementId("pdf-canvas-page-$pageIndex"))?.remove()

        return Uint8Array(buffer).toByteArray().toImageBitmap()
    }

    private fun getOrCreatePageCanvas(pageId: Int): HTMLCanvasElement {
        return (document.getElementById(ElementId("pdf-canvas-page-$pageId")) ?: document.createElement("canvas")
            .apply {
                id = ElementId("pdf-canvas-page-$pageId")
            }) as HTMLCanvasElement
    }
}
