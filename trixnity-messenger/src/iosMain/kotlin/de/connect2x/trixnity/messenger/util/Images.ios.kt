package de.connect2x.trixnity.messenger.util

import net.folivo.trixnity.utils.ByteArrayFlow
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformGetImageDimensionsModule(): Module = module {
    single<GetImageDimensions> {
        GetImageDimensions { byteArrayFlow, maxSize ->
            getImageDimensions(byteArrayFlow, maxSize)
        }
    }
}

suspend fun getImageDimensions(byteArrayFlow: ByteArrayFlow, maxMediaSize: Long): Pair<Int?, Int?> {
    //TODO Implement
    return (null to null)
}
