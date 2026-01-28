package de.connect2x.trixnity.messenger.notification.fcm

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.google.firebase.messaging.FirebaseMessaging
import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.Worker
import de.connect2x.trixnity.messenger.isMatrixMessengerServiceEnabled
import de.connect2x.trixnity.messenger.isMatrixMultiMessengerServiceEnabled
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerConfiguration
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerSettingsHolder
import de.connect2x.trixnity.messenger.notification.NotificationProvider
import de.connect2x.trixnity.messenger.notification.PushNotificationProvider
import de.connect2x.trixnity.messenger.notification.PushNotificationProviderPushKeyUpdater
import de.connect2x.trixnity.messenger.util.ContextGetter
import de.connect2x.trixnity.messenger.util.GetDefaultDeviceDisplayName
import de.connect2x.trixnity.messenger.withMatrixMessengerFromService
import de.connect2x.trixnity.messenger.withMatrixMultiMessengerFromService
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import de.connect2x.trixnity.core.model.UserId
import org.koin.dsl.bind
import org.koin.dsl.module

class FcmPushNotificationProvider(
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
        context.packageManager.setComponentEnabledSetting(
            ComponentName(context, TrixnityMessengerFirebaseMessagingService::class.java),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP,
        )
        SyncAndProcessPendingWorker.enqueueUniquePeriodicWork(context = context)
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            OnNewTokenWorker.enqueueUniqueWork(
                context = context, pushKey = token
            )
        }
    }

    override suspend fun disableService() {
        val context = contextGetter()
        context.packageManager.setComponentEnabledSetting(
            ComponentName(context, TrixnityMessengerFirebaseMessagingService::class.java),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
        )
        SyncAndProcessPendingWorker.stopUniquePeriodicWork(context = context)
    }

    override suspend fun getPusherCustomFields(profile: String?, account: UserId): JsonObject =
        buildJsonObject {
            putJsonObject("default_payload") {
                profile?.let { put("profile", it) }
                put("account", account.full)
            }
        }
}

internal suspend fun <T> withFcmPushNotificationProvider(
    context: Context,
    block: suspend (FcmPushNotificationProvider) -> T
): T = withMatrixMessengerFromService(context) {
    val fcmPushNotificationProvider = it.di.get<FcmPushNotificationProvider>()
    block(fcmPushNotificationProvider)
}

internal suspend fun <T> withFcmPushNotificationProviderPushKeyUpdater(
    context: Context,
    block: suspend (PushNotificationProviderPushKeyUpdater) -> T
): T =
    when {
        isMatrixMultiMessengerServiceEnabled(context) -> withMatrixMultiMessengerFromService(context) {
            block(it.di.get<PushNotificationProviderPushKeyUpdater>())
        }

        isMatrixMessengerServiceEnabled(context) -> withMatrixMessengerFromService(context) {
            block(it.di.get<PushNotificationProviderPushKeyUpdater>())
        }

        else -> throw IllegalStateException("no service enabled")
    }

private fun fcmPushNotificationProviderModule() = module {
    single<FcmPushNotificationProvider> {
        FcmPushNotificationProvider(
            config = get(),
            multiSettings = getOrNull(),
            settings = get(),
            getDefaultDeviceDisplayName = get(),
            matrixClients = get(),
            coroutineScope = get(),
            contextGetter = get(),
        )
    }.apply {
        bind<NotificationProvider>()
        bind<Worker>()
    }
}

private fun fcmPushNotificationProviderPushKeyUpdaterModule() = module {
    single { PushNotificationProviderPushKeyUpdater(getOrNull(), getOrNull()) }
}

fun MatrixMultiMessengerConfiguration.addFcmPushNotificationProvider() {
    modulesFactories += ::fcmPushNotificationProviderPushKeyUpdaterModule
    messengerConfiguration {
        modulesFactories += ::fcmPushNotificationProviderModule
    }
}

fun MatrixMessengerConfiguration.addFcmPushNotificationProvider() {
    modulesFactories += ::fcmPushNotificationProviderPushKeyUpdaterModule
    modulesFactories += ::fcmPushNotificationProviderModule
}
