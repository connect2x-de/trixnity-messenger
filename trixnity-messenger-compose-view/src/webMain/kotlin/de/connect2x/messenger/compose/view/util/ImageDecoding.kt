package de.connect2x.messenger.compose.view.util

import js.function.JsFunction
import kotlinx.coroutines.CompletableDeferred
import web.blob.Blob
import web.blob.BlobPropertyBag
import web.canvas.CanvasRenderingContext2D
import web.canvas.ID
import web.dom.document
import web.events.EventHandler
import web.html.HtmlTagName
import web.html.Image
import web.url.URL

actual suspend fun decodeImageRGBA8888(
    imageData: ByteArray,
    newWidth: Int,
    newHeight: Int
): ByteArray? {
    val result = CompletableDeferred<ByteArray?>()
    val image = Image()
    image.onload = EventHandler {
        val actualWidth = if (newWidth == -1) image.width else newWidth
        val actualHeight = if (newHeight == -1) image.height else newHeight

        val canvas = document.createElement(HtmlTagName.canvas)
        canvas.width = actualWidth
        canvas.height = actualHeight
        val context = canvas.getContext(CanvasRenderingContext2D.ID)!!
        context.drawImage(
            image = image,
            sx = 0.0,
            sy = 0.0,
            sw = image.width.toDouble(),
            sh = image.height.toDouble(),
            dx = 0.0,
            dy = 0.0,
            dw = actualWidth.toDouble(),
            dh = actualHeight.toDouble()
        )
        val imageData = context.getImageData(0, 0, actualWidth, actualHeight).data

        result.complete(ByteArray(imageData.length) { imageData[it].toByte() })
    }
    image.onerror = JsFunction<Nothing?, Any?> { _ -> result.complete(null) }

    val imageUrl = URL.createObjectURL(Blob(arrayOf(imageData), BlobPropertyBag(type = "application/octet-stream")))
    image.src = imageUrl
    val imageData = result.await()
    URL.revokeObjectURL(imageUrl)

    return imageData
}
