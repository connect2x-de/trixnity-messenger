package de.connect2x.trixnity.messenger.util

import com.ashampoo.kim.Kim
import com.ashampoo.kim.format.tiff.constant.TiffTag
import com.ashampoo.kim.model.TiffOrientation
import de.connect2x.lognity.api.logger.Logger
import io.ktor.http.*
import org.koin.core.module.Module
import org.koin.dsl.module
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

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
 * Rotates the data of an image to its Metadata orientation to prevent issues caused by missing interpretation
 * of Exif Data
 */
fun rotateImageToMetadataOrientation(imageBytes: ByteArray, mimeType: ContentType): ByteArray {
    val metadata = Kim.readMetadata(imageBytes)
    val degrees = when (metadata?.findShortValue(TiffTag.TIFF_TAG_ORIENTATION)) {
        TiffOrientation.ROTATE_RIGHT.value.toShort() -> 90
        TiffOrientation.ROTATE_LEFT.value.toShort() -> 270
        TiffOrientation.UPSIDE_DOWN.value.toShort() -> 180
        else -> 0
    }
    if (degrees != 0) {
        try {
            log.debug { "Rotating image by $degrees degrees" }
            val inputStream = ByteArrayInputStream(imageBytes)
            val image = ImageIO.read(inputStream)
            val radians = Math.toRadians(degrees.toDouble())
            val sin = abs(sin(radians))
            val cos = abs(cos(radians))
            val width = floor(image.width * cos + image.height * sin).toInt()
            val height = floor(image.height * cos + image.width * sin).toInt()
            val rotatedImage = BufferedImage(width, height, image.type)
            val transform = AffineTransform().apply {
                this.translate(width.toDouble() / 2, height.toDouble() / 2)
                this.rotate(radians)
                this.translate(-image.width.toDouble() / 2, -image.height.toDouble() / 2)
            }
            val transformOperation = AffineTransformOp(transform, AffineTransformOp.TYPE_BILINEAR)
            transformOperation.filter(image, rotatedImage)
            val outputStream = ByteArrayOutputStream()
            ImageIO.write(rotatedImage, mimeType.contentSubtype, outputStream)
            //Not needed currently since Metadata isn't sent. Might have to be reenabled later, if Metadata sending works to prevent sending of wrong metadata information
            /* val updatedBytes = if (encoded != null) {
            Kim.update(encoded.bytes, MetadataUpdate.Orientation(TiffOrientation.STANDARD)) */
            return outputStream.toByteArray()
        } catch (_: Exception) {
            return imageBytes
        }
    } else return imageBytes
}
