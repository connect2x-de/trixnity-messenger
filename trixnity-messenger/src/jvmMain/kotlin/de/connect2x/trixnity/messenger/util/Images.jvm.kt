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
