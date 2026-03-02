package de.connect2x.trixnity.messenger.notification

import de.connect2x.sysnotify.NotificationIcon
import org.koin.core.module.Module
import org.koin.dsl.module

// TODO: move to webMain for WASM migration, needed here right now to access skiaMain sources
actual fun getPlatformNotificationIconModule(): Module = module {
    single<GetNotificationIcon> {
        object : AbstractGetNotificationIcon() {
            override fun fromResource(path: String): NotificationIcon? = null // Not used/supported in web right now
        }
    }
}
