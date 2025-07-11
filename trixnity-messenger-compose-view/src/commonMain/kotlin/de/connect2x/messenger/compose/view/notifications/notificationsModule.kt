package de.connect2x.messenger.compose.view.notifications

import de.connect2x.sysnotify.NoopNotificationHandler
import de.connect2x.sysnotify.NotificationHandler
import de.connect2x.trixnity.messenger.MatrixMessengerBaseConfiguration
import org.koin.core.module.Module
import org.koin.dsl.module

fun notificationsModule(
    config: MatrixMessengerBaseConfiguration,
    isDebugEnabled: Boolean
): Module = module {
    single<NotificationHandlerProvider> {
        NotificationHandlerProvider.lazy { subId ->
            NotificationHandler(
                name = config.appName,
                id = "$it.$subId",
                appId = config.appId,
                isDebugEnabled = isDebugEnabled
            )
        }
    }
}

fun noopNotificationsModule(): Module = module {
    single<NotificationHandlerProvider> {
        NotificationHandlerProvider.of(NoopNotificationHandler)
    }
}
