package de.connect2x.messenger.compose.view.notifications

import de.connect2x.sysnotify.NoopNotificationHandler
import de.connect2x.sysnotify.NotificationHandler
import de.connect2x.trixnity.messenger.MatrixMessengerBaseConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerCloseHook
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerCloseHook
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerConfiguration
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private val log: KLogger = KotlinLogging.logger {}

@OptIn(ExperimentalUuidApi::class)
fun notificationsModule(
    config: MatrixMessengerBaseConfiguration,
    isDebugEnabled: Boolean
): Module = module {
    val provider = NotificationHandlerProvider.lazy { subId ->
        NotificationHandler(
            name = config.appName,
            id = "${config.appId}.$subId",
            appId = config.appId,
            isDebugEnabled = isDebugEnabled
        )
    }
    single<NotificationHandlerProvider> { provider }
    val hookId = "NotificationHandlerProvider-${Uuid.random().toHexString()}"
    when (config) {
        is MatrixMessengerConfiguration -> single<MatrixMessengerCloseHook>(named(hookId)) {
            MatrixMessengerCloseHook { provider.closeAll() }
        }

        is MatrixMultiMessengerConfiguration -> single<MatrixMultiMessengerCloseHook>(named(hookId)) {
            MatrixMultiMessengerCloseHook { provider.closeAll() }
        }
    }
}

fun noopNotificationsModule(): Module = module {
    log.warn { "You are currently using the noopNotificationsModule and will receive no notifications" }
    single<NotificationHandlerProvider> {
        NotificationHandlerProvider.of(NoopNotificationHandler)
    }
    // No need for a close hook here because NoopNotificationHandler never allocates resources
}
