package de.connect2x.trixnity.messenger.util

import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.folivo.trixnity.utils.ByteArrayFlow
import net.folivo.trixnity.utils.toByteReadChannel
import java.io.IOException
import javax.imageio.ImageIO

actual suspend fun getImageDimensions(byteArrayFlow: ByteArrayFlow, maxMediaSize: Long): Pair<Int?, Int?> {
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
