package de.connect2x.trixnity.messenger.util

import android.graphics.BitmapFactory
import net.folivo.trixnity.utils.ByteArrayFlow
import net.folivo.trixnity.utils.toByteArray
import org.koin.core.module.Module
import org.koin.dsl.module
import net.folivo.trixnity.utils.toByteArray

actual fun platformGetImageDimensionsModule(): Module = module {
    single<GetImageDimensions> {
        GetImageDimensions { byteArrayFlow, maxSize ->
            getImageDimensions(byteArrayFlow, maxSize)
        }
    }
}


suspend fun getImageDimensions(byteArrayFlow: ByteArrayFlow, maxMediaSize: Long): Pair<Int?, Int?> {
    val byteArray = byteArrayFlow.toByteArray(maxMediaSize)
    val bitmap = byteArray?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
    return bitmap?.width to bitmap?.height
}

