package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.utils.ByteArrayFlow
import de.connect2x.trixnity.utils.encodeBase64
import de.connect2x.trixnity.utils.toByteArray
import io.ktor.http.*
import js.promise.Promise
import js.promise.await
import js.promise.invoke
import org.koin.core.module.Module
import org.koin.dsl.module
import web.events.EventHandler
import web.events.EventType
import web.events.addEventListener
import web.html.Image

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
    val base64String = byteArrayFlow.toByteArray(maxMediaSize)?.encodeBase64()
    return base64String?.let {
        val image = Image()
        image.src = "data:$mimeType;base64,${byteArrayFlow.toByteArray().encodeBase64()}"
        Promise(
            executor = { resolve, _ ->
                image.addEventListener(
                    type = EventType("load"),
                    handler = EventHandler { resolve(null) }
                )
            }
        ).await()
        image.width to image.height
    } ?: (null to null)
}

