package de.connect2x.trixnity.messenger.notification

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.core.graphics.scale
import de.connect2x.sysnotify.NotificationIcon
import de.connect2x.sysnotify.fromBitmap
import de.connect2x.trixnity.messenger.util.ContextGetter
import kotlin.coroutines.cancellation.CancellationException
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun getPlatformNotificationIconModule(): Module = module {
    single<GetNotificationIcon> {
        object : GetNotificationIcon {
            override fun fromBytes(encoded: ByteArray, maxWidth: Int, maxHeight: Int): NotificationIcon? {
                try {
                    val bitmap = encoded.decodeToImageBitmap()
                    val width = minOf(bitmap.width, maxWidth)
                    val height = minOf(bitmap.height, maxHeight)
                    val scaledBitmap = bitmap.asAndroidBitmap().scale(width, height)
                    return NotificationIcon.fromBitmap(scaledBitmap)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e else return null
                }
            }

            override fun fromResource(path: String): NotificationIcon {
                val context = get<ContextGetter>()()
                return context.assets.open(path).use { stream ->
                    NotificationIcon.fromBitmap(BitmapFactory.decodeStream(stream))
                }
            }
        }
    }
}
