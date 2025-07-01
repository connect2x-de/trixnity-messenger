package de.connect2x.messenger.compose.view.util

import kotlinx.coroutines.CompletableDeferred
import web.blob.Blob
import web.blob.BlobPropertyBag
import web.canvas.CanvasRenderingContext2D
import web.dom.document
import web.events.EventHandler
import web.html.Image
import web.url.URL

actual suspend fun decodeImageRGBA8888(imageData: ByteArray, newWidth: Int, newHeight: Int): ByteArray? {
    val image = Image().apply {
        src = URL.createObjectURL(Blob(arrayOf(imageData), BlobPropertyBag(type = "application/octet-stream")))
    }
    val actualWidth = if (newWidth == -1) image.width else newWidth
    val actualHeight = if (newHeight == -1) image.height else newHeight
    val result = CompletableDeferred<ByteArray?>()
    image.onload = EventHandler { event ->
        val canvas = document.createElement("canvas").asDynamic()
        canvas.width = actualWidth
        canvas.height = actualHeight
        val context = canvas.getContext("2d") as CanvasRenderingContext2D
        context.drawImage(image, 0.0, 0.0)
        val imageDataSize = actualWidth * actualHeight * 4
        val imageData = ByteArray(imageDataSize)
        val imageDataView = context.getImageData(0, 0, actualWidth, actualHeight).data
        for (i in 0..<imageDataSize) {
            imageData[i] = imageDataView[i].toByte()
        }
        result.complete(imageData)
    }
    return result.await()
}
