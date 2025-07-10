package de.connect2x.trixnity.messenger.util

import io.ktor.http.ContentType
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

suspend fun getImageDimensions(byteArrayFlow: ByteArrayFlow, maxMediaSize: Long, mimeType: ContentType?): Pair<Int?, Int?> {
    //TODO Implement
    return (null to null)
}
