package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.utils.ByteArrayFlow
import de.connect2x.trixnity.utils.toByteArray
import io.ktor.http.ContentType
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIImage

actual fun platformGetImageDimensionsModule(): Module = module {
    single<GetImageDimensions> {
        GetImageDimensions { byteArrayFlow, maxSize, mimeType -> getImageDimensions(byteArrayFlow, maxSize, mimeType) }
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
suspend fun getImageDimensions(
    byteArrayFlow: ByteArrayFlow,
    maxMediaSize: Long,
    mimeType: ContentType?,
): Pair<Int?, Int?> {
    val bytes = byteArrayFlow.toByteArray(maxMediaSize) ?: return (null to null)
    val imageData = bytes.usePinned { NSData.create(it.addressOf(0), bytes.size.toULong()) }
    UIImage(data = imageData).let { image ->
        val width = image.size.useContents { (width * image.scale).toInt() }
        val height = image.size.useContents { (height * image.scale).toInt() }
        return (width to height)
    }
}
