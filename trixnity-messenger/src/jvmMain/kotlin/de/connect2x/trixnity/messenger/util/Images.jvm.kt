package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.utils.ByteArrayFlow
import de.connect2x.trixnity.utils.toByteReadChannel
import io.ktor.http.*
import io.ktor.utils.io.jvm.javaio.*
import javax.imageio.ImageIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformGetImageDimensionsModule(): Module = module {
    single<GetImageDimensions> {
        GetImageDimensions { byteArrayFlow, maxSize, mimeType -> getImageDimensions(byteArrayFlow, maxSize, mimeType) }
    }
}

suspend fun getImageDimensions(
    byteArrayFlow: ByteArrayFlow,
    maxMediaSize: Long,
    mimeType: ContentType?,
): Pair<Int?, Int?> {
    val inputStream = byteArrayFlow.toByteReadChannel().toInputStream()
    return withContext(Dispatchers.IO) {
        try {
            val image = ImageIO.read(inputStream)
            image.width to image.height
        } catch (_: Exception) {
            null to null
        }
    }
}
