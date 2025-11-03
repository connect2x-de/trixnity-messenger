package de.connect2x.trixnity.messenger.notification

import android.content.Intent
import androidx.core.net.toUri
import de.connect2x.sysnotify.NotificationHandler
import de.connect2x.sysnotify.withActivationFactory
import de.connect2x.sysnotify.withActivity
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
        ) { name, id, isDebugEnabled, appId ->
            NotificationHandler(
                name = name,
                id = id,
                isDebugEnabled = isDebugEnabled,
                appId = appId,
            ).withContext { get<ContextGetter>().invoke() }
                .withActivity { get<ActivityGetter>().invoke() }
                .withActivationFactory { _, notification ->
                    Intent(
                        Intent.ACTION_VIEW,
                        "${config.urlProtocol}://localhost/${notification.callbackData}".toUri(),
                    )
                }
        }
    }.apply {
        bind<AutoCloseable>()
        bind<Worker>()
    }
}
