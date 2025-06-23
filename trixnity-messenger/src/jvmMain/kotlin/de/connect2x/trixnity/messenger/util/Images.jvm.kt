package de.connect2x.trixnity.messenger.util

import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.folivo.trixnity.utils.ByteArrayFlow
import net.folivo.trixnity.utils.toByteReadChannel
import org.koin.core.module.Module
import org.koin.dsl.module
import javax.imageio.ImageIO

actual fun platformGetImageDimensionsModule(): Module = module {
    single<GetImageDimensions> {
        GetImageDimensions { byteArrayFlow, maxSize ->
            getImageDimensions(byteArrayFlow, maxSize)
        }
    }
}

suspend fun getImageDimensions(byteArrayFlow: ByteArrayFlow, maxMediaSize: Long): Pair<Int?, Int?> {
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
