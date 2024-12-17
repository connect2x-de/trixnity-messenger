@file:JsModule("pdfjs-dist")
@file:JsNonModule

package externals.pdfjs

import js.promise.Promise
import web.canvas.CanvasRenderingContext2D
import web.url.URL

internal external class GlobalWorkerOptions {
    companion object {
        var workerSrc: String
    }
}

internal external fun getDocument(src: URL): PDFDocumentLoadingTask

internal external interface PDFDocumentLoadingTask {
    val promise: Promise<PDFDocumentProxy>
}

internal external interface PDFDocumentProxy {
    val numPages: Int
    fun getPage(page: Int): Promise<PDFPageProxy>
}

internal external interface PDFPageProxy {
    fun render(params: PDFRenderParams): PDFDocumentLoadingTask
    fun getViewport(params: GetViewportParameters): PDFPageViewport
}

internal external interface PDFRenderParams {
    var canvasContext: CanvasRenderingContext2D
    var viewport: PDFPageViewport
}

internal external interface PDFPageViewport {
    val width: Int
    val height: Int
}

internal external interface GetViewportParameters {
    var scale: Float
}
