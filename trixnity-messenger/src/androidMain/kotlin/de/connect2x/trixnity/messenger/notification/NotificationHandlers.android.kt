package de.connect2x.trixnity.messenger.notification

import android.content.Intent
import androidx.core.net.toUri
import de.connect2x.sysnotify.NotificationHandler
import de.connect2x.sysnotify.withActivationIntent
import de.connect2x.sysnotify.withActivity
import de.connect2x.sysnotify.withChannel
import de.connect2x.sysnotify.withContext
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.Worker
import de.connect2x.trixnity.messenger.util.ActivityGetter
import de.connect2x.trixnity.messenger.util.ContextGetter
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

actual fun platformNotificationHandlersModule(): Module = module {
    single<NotificationHandlers> {
            val config = get<MatrixMessengerConfiguration>()
            NotificationHandlersImpl(
                config = config,
                notificationProviders = get(),
                multiSettings = getOrNull(),
                matrixClients = get(),
            ) { name, id, appId, contributesToCounter ->
                NotificationHandler(name = name, id = id, appId = appId, contributesToCounter = contributesToCounter)
                    .withContext { get<ContextGetter>()() }
                    .withActivity { get<ActivityGetter>()() }
                    .withActivationIntent { _, notification ->
                        Intent(Intent.ACTION_VIEW, notification.callbackData?.toUri())
                    }
                    .withChannel()
            }
        }
        .apply {
            bind<AutoCloseable>()
            bind<Worker>()
        }
}
