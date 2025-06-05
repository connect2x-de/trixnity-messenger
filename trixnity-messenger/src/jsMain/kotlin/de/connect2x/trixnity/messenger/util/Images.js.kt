package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.viewmodel.util.limitedByteArrayOrNull
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.await
import net.folivo.trixnity.utils.ByteArrayFlow
import net.folivo.trixnity.utils.toByteArray
import org.koin.core.module.Module
import org.koin.dsl.module
import org.w3c.dom.Image
import kotlin.js.Promise

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
    val base64String = byteArrayFlow.limitedByteArrayOrNull(maxMediaSize)?.encodeBase64()
    return base64String?.let {
        val image = Image()
        image.src = "data:$mimeType;base64,${byteArrayFlow.toByteArray().encodeBase64()}"
        Promise(executor = { resolve, reject -> image.addEventListener("load", resolve) }).await()
        image.width to image.height
    } ?: (null to null)
}

