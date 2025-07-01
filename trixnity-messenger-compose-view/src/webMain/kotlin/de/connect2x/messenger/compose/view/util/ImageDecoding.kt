package de.connect2x.messenger.compose.view.util

import js.typedarrays.toUint8Array
import kotlinx.coroutines.CompletableDeferred
import web.blob.Blob
import web.blob.BlobPropertyBag
import web.canvas.CanvasRenderingContext2D
import web.dom.document
import web.events.Event
import web.events.EventHandler
import web.html.HTMLCanvasElement
import web.html.HtmlTagName
import web.html.Image
import web.url.URL

actual suspend fun decodeImageRGBA8888(imageData: ByteArray, newWidth: Int, newHeight: Int): ByteArray? {
    // @formatter:off
    val objectUrl = URL.createObjectURL(Blob(
        blobParts = arrayOf(imageData.toUint8Array()),
        options = BlobPropertyBag(type = "application/octet-stream")
    ))
    // @formatter:on
    val result = CompletableDeferred<ByteArray?>()
    val image = Image()
    val actualWidth = if (newWidth == -1) image.width else newWidth
    val actualHeight = if (newHeight == -1) image.height else newHeight

    image.onload = EventHandler { event ->
        val canvas = document.createElement(HtmlTagName<HTMLCanvasElement>("canvas"))
        canvas.width = actualWidth
        canvas.height = actualHeight
        val context = canvas.getContext(CanvasRenderingContext2D.ID)!!
        context.drawImage(image, 0.0, 0.0, actualWidth.toDouble(), actualHeight.toDouble())
        val imageDataSize = actualWidth * actualHeight * 4
        val imageData = ByteArray(imageDataSize)
        val imageDataView = context.getImageData(0, 0, actualWidth, actualHeight).data
        for (i in 0..<imageDataSize) {
            imageData[i] = imageDataView[i].toByte()
        }
        result.complete(imageData)
        Unit
    }
    image.onerror = { event: Event -> result.complete(null) }.asDynamic()

    image.src = objectUrl
    val decodedImageData = result.await()
    URL.revokeObjectURL(objectUrl)

    return decodedImageData
}
