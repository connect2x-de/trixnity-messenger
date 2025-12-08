package de.connect2x.messenger.compose.view.files

import androidx.compose.ui.graphics.ImageBitmap
import io.github.oshai.kotlinlogging.KotlinLogging
import js.promise.await
import js.typedarrays.Uint8Array
import js.typedarrays.toByteArray
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
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
import web.url.URL
import kotlin.coroutines.resume

private val log = KotlinLogging.logger { }

suspend fun PdfReaderWeb(blob: Blob): PdfReaderWeb {
    val doc = getDocument(URL(URL.createObjectURL(blob))).promise.await()
    val documentWidth = doc.getPageAsync(1).await().getViewport(GetViewportParameters(1f)).width.toInt()
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

        val page = pdfDocument.getPageAsync(pageIndex).await()
        val scaledViewport = page.getViewport(GetViewportParameters(scale = scale))
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

private suspend fun HTMLCanvasElement.toBlob(): Blob? = suspendCancellableCoroutine { continuation ->
    toBlob(callback = { blob -> continuation.resume(blob) })
}
