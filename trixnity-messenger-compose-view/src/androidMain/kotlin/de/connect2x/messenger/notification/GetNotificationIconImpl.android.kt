package de.connect2x.trixnity.messenger.notification

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.core.graphics.scale
import de.connect2x.sysnotify.NotificationIcon
import de.connect2x.sysnotify.fromBitmap
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
                    val scaledBitmap = bitmap.asAndroidBitmap().scale(width, height)
                    return NotificationIcon.fromBitmap(scaledBitmap)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    else return null
                }
            }
        }
    }
}

