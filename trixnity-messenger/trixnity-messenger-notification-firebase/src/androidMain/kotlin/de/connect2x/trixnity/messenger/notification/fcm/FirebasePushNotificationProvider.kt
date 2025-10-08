package de.connect2x.trixnity.messenger.notification.fcm

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerConfiguration
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerSettingsHolder
import de.connect2x.trixnity.messenger.notification.NotificationProvider
import de.connect2x.trixnity.messenger.notification.NotificationProviders
import de.connect2x.trixnity.messenger.notification.PushNotificationProvider
import de.connect2x.trixnity.messenger.util.ContextGetter
import de.connect2x.trixnity.messenger.util.GetDefaultDeviceDisplayName
import de.connect2x.trixnity.messenger.withMatrixMessengerFromService
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import net.folivo.trixnity.core.model.UserId
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

class FirebasePushNotificationProvider(
    config: MatrixMessengerConfiguration,
    multiSettings: MatrixMultiMessengerSettingsHolder?,
    settings: MatrixMessengerSettingsHolder,
    getDefaultDeviceDisplayName: GetDefaultDeviceDisplayName,
    matrixClients: MatrixClients,
    coroutineScope: CoroutineScope,
    private val contextGetter: ContextGetter,
) : PushNotificationProvider(
    config = config,
    multiSettings = multiSettings,
    settings = settings,
    getDefaultDeviceDisplayName = getDefaultDeviceDisplayName,
    matrixClients = matrixClients,
    coroutineScope = coroutineScope,
) {
    companion object {
        const val ID = "de.connect2x.trixnity.messenger.notification.fcm"
    }

    override val id: String = ID
    override val displayName: String = "Google Firebase Cloud Messaging"

    override suspend fun enableService() {
        val context = contextGetter()
        SyncAndProcessPendingWorker.enqueueUniquePeriodicWork(context = context)
        context.packageManager.setComponentEnabledSetting(
            ComponentName(context, TrixnityMessengerFirebaseMessagingService::class.java),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP,
        )
    }

    override suspend fun disableService() {
        val context = contextGetter()
        SyncAndProcessPendingWorker.stopUniquePeriodicWork(context = context)
        context.packageManager.setComponentEnabledSetting(
            ComponentName(context, TrixnityMessengerFirebaseMessagingService::class.java),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
        )
    }

    override suspend fun getPusherCustomFields(profile: String?, account: UserId): JsonObject =
        buildJsonObject {
            putJsonObject("default_payload") {
                profile?.let { put("profile", it) }
                put("account", account.full)
            }
        }
}

internal suspend fun <T> withFirebasePushNotificationProvider(
    context: Context,
    block: suspend (FirebasePushNotificationProvider) -> T
): T = withMatrixMessengerFromService(context) {
    val firebasePushNotificationProvider = it.di.get<NotificationProviders>()
        .first { it is FirebasePushNotificationProvider } as? FirebasePushNotificationProvider
    if (firebasePushNotificationProvider != null) block(firebasePushNotificationProvider)
    else throw IllegalStateException("FirebasePushNotificationProvider not found in DI")
}

fun MatrixMultiMessengerConfiguration.addFirebasePushNotificationProvider() {
    messengerConfiguration {
        addFirebasePushNotificationProvider()
    }
}

fun MatrixMessengerConfiguration.addFirebasePushNotificationProvider() {
    modulesFactories += {
        module {
            single<FirebasePushNotificationProvider>(named<FirebasePushNotificationProvider>()) {
                FirebasePushNotificationProvider(
                    config = get(),
                    multiSettings = getOrNull(),
                    settings = get(),
                    getDefaultDeviceDisplayName = get(),
                    matrixClients = get(),
                    coroutineScope = get(),
                    contextGetter = get(),
                )
            }.bind<NotificationProvider>()
        }
    }
}
