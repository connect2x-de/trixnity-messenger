package de.connect2x.trixnity.messenger.notification

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import de.connect2x.sysnotify.NotificationIcon
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.FilterMipmap
import org.jetbrains.skia.FilterMode
import org.jetbrains.skia.Image
import org.jetbrains.skia.MipmapMode
import org.koin.core.module.Module
import org.koin.dsl.module
import kotlin.coroutines.cancellation.CancellationException

actual fun getPlatformNotificationIconModule(): Module = module {
    single<GetNotificationIcon> {
        object : GetNotificationIcon {
            override suspend fun invoke(encoded: ByteArray, maxWidth: Int, maxHeight: Int): NotificationIcon? {
                try {
                    val bitmap = encoded.decodeToImageBitmap()
                    val width = minOf(bitmap.width, maxWidth)
                    val height = minOf(bitmap.height, maxHeight)
                    val scaledBitmap = bitmap.scale(width, height) ?: return null
                    val dataSize = scaledBitmap.width * scaledBitmap.height
                    val argbIcon = IntArray(dataSize)
                    val rgbaIcon = ByteArray(dataSize * 4)
                    scaledBitmap.readPixels(argbIcon)
                    for (i in argbIcon.indices) {
                        val pixel = argbIcon[i]
                        val componentIndex = i * 4
                        rgbaIcon[componentIndex] = ((pixel shr 16) and 0xFF).toByte()
                        rgbaIcon[componentIndex + 1] = ((pixel shr 8) and 0xFF).toByte()
                        rgbaIcon[componentIndex + 2] = (pixel and 0xFF).toByte()
                        rgbaIcon[componentIndex + 3] = ((pixel shr 24) and 0xFF).toByte()
                    }
                    return NotificationIcon(rgbaIcon, scaledBitmap.width, scaledBitmap.height)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    else return null
                }
            }

            private fun ImageBitmap.scale(width: Int, height: Int): ImageBitmap? {
                val image = Image.makeFromBitmap(asSkiaBitmap())
                val scaled = image.scale(width, height)
                return scaled?.toComposeImageBitmap()
            }

            private fun Image.scale(width: Int, height: Int): Image? {
                val bitmap = Bitmap()
                bitmap.allocN32Pixels(width, height)
                scalePixels(
                    bitmap.peekPixels() ?: return null,
                    FilterMipmap(FilterMode.LINEAR, MipmapMode.LINEAR),
                    false
                )
                return Image.makeFromBitmap(bitmap)
            }
        }
    }
}

