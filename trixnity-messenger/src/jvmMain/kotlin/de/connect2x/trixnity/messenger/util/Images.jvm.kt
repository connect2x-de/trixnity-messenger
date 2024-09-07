package de.connect2x.trixnity.messenger.util

import com.ashampoo.kim.Kim
import com.ashampoo.kim.format.tiff.constant.TiffTag
import com.ashampoo.kim.model.MetadataUpdate
import com.ashampoo.kim.model.TiffOrientation
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.ContentType
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.folivo.trixnity.utils.ByteArrayFlow
import net.folivo.trixnity.utils.toByteReadChannel
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import java.io.IOException
import javax.imageio.ImageIO

private val log = KotlinLogging.logger { }
actual suspend fun getImageDimensions(byteArrayFlow: ByteArrayFlow): Pair<Int?, Int?> {
    val inputStream = byteArrayFlow.toByteReadChannel().toInputStream()
    return withContext(Dispatchers.IO) {
        try {
            val image = ImageIO.read(inputStream)
            image.width to image.height
        } catch (ioException: IOException) {
            null to null
        }
    }
}

actual suspend fun rotateImageToMetadataOrientation(imageBytes: ByteArray, mimeType: ContentType): ByteArray {
    //TODO Make rotation dependent on file size because of in Memory operation
    val metadata = Kim.readMetadata(imageBytes)
    val degrees = when (metadata?.findShortValue(TiffTag.TIFF_TAG_ORIENTATION)) {
        TiffOrientation.ROTATE_RIGHT.value.toShort() -> 90
        TiffOrientation.ROTATE_LEFT.value.toShort() -> 270
        TiffOrientation.UPSIDE_DOWN.value.toShort() -> 180
        else -> 0
    }
    try {
        val image = Image.makeFromEncoded(imageBytes)
        val bitmap = Bitmap.makeFromImage(image)
        log.debug { "Rotating image by $degrees degrees" }
        val encoded = Image.makeFromBitmap(bitmap).encodeToData(EncodedImageFormat.PNG)
        val updatedBytes = if (encoded != null) {
            Kim.update(encoded.bytes, MetadataUpdate.Orientation(TiffOrientation.STANDARD))
        }
        else imageBytes
        return updatedBytes
    }
    catch (_ : Exception) {
        return imageBytes
    }
}
