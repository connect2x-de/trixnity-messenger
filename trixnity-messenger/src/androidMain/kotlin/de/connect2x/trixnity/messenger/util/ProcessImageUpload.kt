package de.connect2x.trixnity.messenger.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import com.ashampoo.kim.Kim
import com.ashampoo.kim.format.tiff.constant.TiffTag
import com.ashampoo.kim.model.TiffOrientation
import de.connect2x.lognity.api.logger.Logger
import io.ktor.http.*
import io.ktor.http.ContentType.*
import java.io.ByteArrayOutputStream
import org.koin.core.module.Module
import org.koin.dsl.module

private val log: Logger = Logger("de.connect2x.trixnity.messenger.util.ProcessImageUploadKt")

actual fun platformProcessImageUploadModule(): Module = module {
    single<ProcessImageUpload> {
        ProcessImageUpload { imageBytes, mimeType ->
            val rotated = rotateImageToMetadataOrientation(imageBytes, mimeType)
            removeImageMetadata(rotated)
        }
    }
}

/**
 * Rotates the data of an image to its Metadata orientation to prevent issues caused by missing interpretation of Exif
 * Data
 */
fun rotateImageToMetadataOrientation(imageBytes: ByteArray, mimeType: ContentType): ByteArray {
    val metadata = Kim.readMetadata(imageBytes)
    val degrees =
        when (metadata?.findShortValue(TiffTag.TIFF_TAG_ORIENTATION)) {
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
            val compressMimeType =
                when {
                    mimeType.match(Image.PNG) -> Bitmap.CompressFormat.PNG
                    mimeType.match(Image.JPEG) -> Bitmap.CompressFormat.JPEG
                    mimeType.match("image/webp") &&
                        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R ->
                        Bitmap.CompressFormat.WEBP_LOSSLESS
                    else -> Bitmap.CompressFormat.PNG
                }
            if (!rotatedBitmap.compress(compressMimeType, 100, byteOutput)) {
                return imageBytes
            }
            // Not needed currently since Metadata isn't sent. Might have to be reenabled later, if Metadata sending
            // works to prevent sending of wrong metadata information
            // val updatedBytes = Kim.update(byteOutput.toByteArray(),
            // MetadataUpdate.Orientation(TiffOrientation.STANDARD))
            return byteOutput.toByteArray()
        } else return imageBytes
    } catch (_: Exception) {
        return imageBytes
    }
}
