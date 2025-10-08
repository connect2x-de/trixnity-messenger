package de.connect2x.trixnity.messenger.notification

import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

actual fun platformNotificationHandlersModule(): Module = module {
    single<NotificationHandlers> {
        NotificationHandlersImpl(
            config = get(),
            notificationProviders = get(),
            multiSettings = getOrNull()
        )
    }.bind<AutoCloseable>()
}
