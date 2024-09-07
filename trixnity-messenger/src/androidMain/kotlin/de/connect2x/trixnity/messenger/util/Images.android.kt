package de.connect2x.trixnity.messenger.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import com.ashampoo.kim.Kim
import com.ashampoo.kim.format.tiff.constant.TiffTag
import com.ashampoo.kim.model.MetadataUpdate
import com.ashampoo.kim.model.TiffOrientation
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.ContentType
import io.ktor.http.ContentType.Image
import net.folivo.trixnity.utils.ByteArrayFlow
import net.folivo.trixnity.utils.toByteArray
import java.io.ByteArrayOutputStream

private val log = KotlinLogging.logger{ }
actual suspend fun getImageDimensions(byteArrayFlow: ByteArrayFlow): Pair<Int?, Int?> {
    val byteArray = byteArrayFlow.toByteArray()
    val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    return bitmap?.width to bitmap?.height
}

actual suspend fun rotateImageToMetadataOrientation(imageBytes : ByteArray, mimeType: ContentType) : ByteArray {
    //TODO Make rotation dependent on file size because of in Memory operation
    val metadata = Kim.readMetadata(imageBytes)
    val degrees = when (metadata?.findShortValue(TiffTag.TIFF_TAG_ORIENTATION)) {
        TiffOrientation.ROTATE_RIGHT.value.toShort() -> 90
        TiffOrientation.ROTATE_LEFT.value.toShort() -> 270
        TiffOrientation.UPSIDE_DOWN.value.toShort() -> 180
        else -> 0
    }
    try {
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        log.debug { "Rotating image by $degrees degrees" }
        if (bitmap != null) {
            val rotationMatrix = Matrix()
            rotationMatrix.postRotate(degrees.toFloat())
            val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, rotationMatrix, true)
            val byteOutput = ByteArrayOutputStream()
            val compressMimeType = when {
                mimeType.match(Image.PNG) -> Bitmap.CompressFormat.PNG
                mimeType.match(Image.JPEG) -> Bitmap.CompressFormat.JPEG
                else -> Bitmap.CompressFormat.PNG
            }
            rotatedBitmap.compress(compressMimeType, 100, byteOutput)
            val updatedBytes = Kim.update(byteOutput.toByteArray(), MetadataUpdate.Orientation(TiffOrientation.STANDARD))
            return updatedBytes
        } else return imageBytes
    }
    catch (_:Exception) {
        return imageBytes
    }
}
