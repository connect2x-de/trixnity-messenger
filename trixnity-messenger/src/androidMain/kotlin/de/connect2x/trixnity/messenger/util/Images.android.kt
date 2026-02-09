package de.connect2x.trixnity.messenger.util

import android.graphics.BitmapFactory
import io.ktor.http.*
import de.connect2x.trixnity.utils.ByteArrayFlow
import de.connect2x.trixnity.utils.toByteArray
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformGetImageDimensionsModule(): Module = module {
    single<GetImageDimensions> {
        GetImageDimensions { byteArrayFlow, maxSize, mimeType ->
            getImageDimensions(byteArrayFlow, maxSize, mimeType)
        }
    }
}


suspend fun getImageDimensions(
    byteArrayFlow: ByteArrayFlow,
    maxMediaSize: Long,
    mimeType: ContentType?
): Pair<Int?, Int?> {
    val byteArray = byteArrayFlow.toByteArray(maxMediaSize)
    val bitmap = byteArray?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
    return bitmap?.width to bitmap?.height
}

