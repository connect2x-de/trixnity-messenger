package de.connect2x.trixnity.messenger.notification

import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module
import platform.UIKit.UIApplication
import platform.UIKit.registerForRemoteNotifications
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

actual fun platformNotificationHandlersModule(): Module = module {
    single<NotificationHandlers> {
        NotificationHandlersImpl(
            config = get(),
            notificationProviders = get(),
            multiSettings = getOrNull(),
            requestPermissionsCallback = { granted ->
                if (granted) {
                    dispatch_async(dispatch_get_main_queue()) {
                        UIApplication.sharedApplication.registerForRemoteNotifications()
                    }
                }
            })
    }.bind<AutoCloseable>()
}

