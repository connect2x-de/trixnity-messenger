package de.connect2x.trixnity.messenger.notification

import de.connect2x.sysnotify.NotificationIcon
import de.connect2x.sysnotify.fromBufferedImage
import org.koin.core.module.Module
import org.koin.dsl.module
import javax.imageio.ImageIO

actual fun getPlatformNotificationIconModule(): Module = module {
    single<GetNotificationIcon> {
        object : AbstractGetNotificationIcon() {
            override fun fromResource(path: String): NotificationIcon {
                return this::class.java.getResourceAsStream("/$path").use { stream ->
                    NotificationIcon.fromBufferedImage(ImageIO.read(stream))
                }
            }
        }
    }
}
