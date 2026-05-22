package de.connect2x.trixnity.messenger.notification

import de.connect2x.trixnity.messenger.Worker
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

fun notificationModule(): Module = module {
    single<NotificationSyncService> {
            NotificationSyncService(
                matrixClients = get(),
                notificationHandlers = get(),
                notificationProviders = get(),
                config = get(),
                settings = get(),
                roomName = get(),
                getNotificationIcon = getOrNull(),
                i18n = get(),
            )
        }
        .bind<Worker>()
    single<NotificationProviders> {
        NotificationProviders(getAll<NotificationProvider>()) {
            NoOpNotificationProvider(settings = get(), coroutineScope = get())
        }
    }
    includes(platformNotificationHandlersModule())
}
