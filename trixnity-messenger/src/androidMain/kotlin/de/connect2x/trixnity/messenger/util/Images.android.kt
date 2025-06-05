package de.connect2x.trixnity.messenger.util

import android.graphics.BitmapFactory
import de.connect2x.trixnity.messenger.viewmodel.util.limitedByteArrayOrNull
import io.ktor.http.*
import net.folivo.trixnity.utils.ByteArrayFlow
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
    val byteArray = byteArrayFlow.limitedByteArrayOrNull(maxMediaSize)
    val bitmap = byteArray?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
    return bitmap?.width to bitmap?.height
}

