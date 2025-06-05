package de.connect2x.trixnity.messenger.util

import io.ktor.http.ContentType
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.folivo.trixnity.utils.ByteArrayFlow
import net.folivo.trixnity.utils.toByteReadChannel
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.IOException
import javax.imageio.ImageIO

actual fun platformGetImageDimensionsModule(): Module = module {
    single<GetImageDimensions> {
        GetImageDimensions { byteArrayFlow, maxSize, mimeType ->
            getImageDimensions(byteArrayFlow, maxSize, mimeType)
        }
    }
}

suspend fun getImageDimensions(byteArrayFlow: ByteArrayFlow, maxMediaSize: Long, mimeType: ContentType?): Pair<Int?, Int?> {
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
