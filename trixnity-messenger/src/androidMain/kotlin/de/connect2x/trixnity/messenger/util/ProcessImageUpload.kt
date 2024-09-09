package de.connect2x.trixnity.messenger.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import com.ashampoo.kim.Kim
import com.ashampoo.kim.format.tiff.constant.TiffTag
import com.ashampoo.kim.model.TiffOrientation
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.ContentType
import io.ktor.http.ContentType.Image
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.ByteArrayOutputStream

val log = KotlinLogging.logger { }

actual fun platformProcessImageUploadModule(): Module = module {
    single<ProcessImageUpload> {
        ProcessImageUpload { imageBytes, mimeType ->
            rotateImageToMetadataOrientation(imageBytes, mimeType)
        }
    }
}

suspend fun rotateImageToMetadataOrientation(imageBytes: ByteArray, mimeType: ContentType): ByteArray {
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
            //Not needed currently since Metadata isn't sent. Might have to be reenabled later, if Metadata sending works to prevent sending of wrong metadata information
            //val updatedBytes = Kim.update(byteOutput.toByteArray(), MetadataUpdate.Orientation(TiffOrientation.STANDARD))
            return byteOutput.toByteArray()
        } else return imageBytes
    } catch (_: Exception) {
        return imageBytes
    }
}
