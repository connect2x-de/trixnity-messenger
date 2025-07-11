package de.connect2x.messenger.compose.view.notifications

import de.connect2x.sysnotify.NoopNotificationHandler
import de.connect2x.sysnotify.NotificationHandler
import de.connect2x.trixnity.messenger.MatrixMessengerBaseConfiguration
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.module.Module
import org.koin.dsl.module

private val log: KLogger = KotlinLogging.logger {}

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
        log.warn { "You are currently using the noopNotificationsModule and will receive no notifications" }
        NotificationHandlerProvider.of(NoopNotificationHandler)
    }
}
