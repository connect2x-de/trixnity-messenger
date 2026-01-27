package de.connect2x.trixnity.messenger.notification.fcm

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.google.firebase.messaging.FirebaseMessaging
import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.MatrixMessengerAccountSettings
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerSettings
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.Worker
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerConfiguration
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerSettings
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerSettingsHolder
import de.connect2x.trixnity.messenger.notification.NotificationProvider
import de.connect2x.trixnity.messenger.notification.PushNotificationProvider
import de.connect2x.trixnity.messenger.notification.PushNotificationProvider.PusherSettings
import de.connect2x.trixnity.messenger.settings.NestedSettingsView
import de.connect2x.trixnity.messenger.settings.SettingsView
import de.connect2x.trixnity.messenger.settings.settingsView
import de.connect2x.trixnity.messenger.update
import de.connect2x.trixnity.messenger.util.ContextGetter
import de.connect2x.trixnity.messenger.util.GetDefaultDeviceDisplayName
import de.connect2x.trixnity.messenger.withMatrixMessengerFromService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import net.folivo.trixnity.core.model.UserId
import org.koin.dsl.bind
import org.koin.dsl.module
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

@Serializable
@NestedSettingsView("notification", "provider", "fcm")
data class MatrixMultiMessengerNotificationProviderFcmSettings(
    val pusher: PusherSettings? = null,
) : SettingsView<MatrixMultiMessengerSettings>

val MatrixMultiMessengerSettings.notificationProviderFcm
        by settingsView<MatrixMultiMessengerSettings, MatrixMultiMessengerNotificationProviderFcmSettings>()

@Serializable
@NestedSettingsView("notification", "provider", "fcm")
data class MatrixMessengerNotificationProviderFcmSettings(
    val pusher: PusherSettings? = null,
) : SettingsView<MatrixMessengerSettings>

val MatrixMessengerSettings.notificationProviderFcm
        by settingsView<MatrixMessengerSettings, MatrixMessengerNotificationProviderFcmSettings>()

@Serializable
@NestedSettingsView("notification", "provider", "fcm")
data class MatrixMessengerAccountNotificationProviderFcmSettings(
    val enabled: Boolean = false,
    val deliveredPusher: PusherSettings? = null,
) : SettingsView<MatrixMessengerAccountSettings>

val MatrixMessengerAccountSettings.notificationProviderFcm
        by settingsView<MatrixMessengerAccountSettings, MatrixMessengerAccountNotificationProviderFcmSettings>()

data class FcmPushNotificationProviderConfig(
    val pushAppId: String,
    val pushUrl: String,
    val periodicSyncInterval: Duration,
)

class FcmPushNotificationProvider(
    val config: FcmPushNotificationProviderConfig,
    messengerConfig: MatrixMessengerConfiguration,
    multiSettings: MatrixMultiMessengerSettingsHolder?,
    settings: MatrixMessengerSettingsHolder,
    getDefaultDeviceDisplayName: GetDefaultDeviceDisplayName,
    matrixClients: MatrixClients,
    coroutineScope: CoroutineScope,
    private val contextGetter: ContextGetter,
) : PushNotificationProvider(
    pushAppId = config.pushAppId,
    config = messengerConfig,
    multiSettings = multiSettings,
    settings = settings,
    getDefaultDeviceDisplayName = getDefaultDeviceDisplayName,
    matrixClients = matrixClients,
    coroutineScope = coroutineScope,
) {
    override val id = "de.connect2x.trixnity.messenger.notification.fcm"
    override val displayName: String = "Google Firebase Cloud Messaging"

    override val currentPusherSettings =
        (multiSettings?.map { s -> s.notificationProviderFcm.pusher }
            ?: settings.map { s -> s.notificationProviderFcm.pusher })
            .shareIn(coroutineScope, SharingStarted.Eagerly, replay = 1)
    override val MatrixMessengerAccountSettings.pusherSettings: PusherAccountSettings
        get() = notificationProviderFcm.run {
            PusherAccountSettings(
                enabled = enabled,
                deliveredPusher = deliveredPusher,
            )
        }

    override suspend fun MatrixMessengerSettingsHolder.updatePusherSettings(
        account: UserId,
        updater: (PusherAccountSettings) -> PusherAccountSettings
    ) {
        update<MatrixMessengerAccountNotificationProviderFcmSettings>(account) {
            val updateResult = updater(
                PusherAccountSettings(
                    enabled = it.enabled,
                    deliveredPusher = it.deliveredPusher,
                )
            )
            it.copy(
                enabled = updateResult.enabled,
                deliveredPusher = updateResult.deliveredPusher,
            )
        }
    }

    override suspend fun enableService() {
        val context = contextGetter()
        context.packageManager.setComponentEnabledSetting(
            ComponentName(context, TrixnityMessengerFirebaseMessagingService::class.java),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP,
        )
        SyncAndProcessPendingWorker.enqueueUniquePeriodicWork(
            context = context,
            interval = config.periodicSyncInterval
        )
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

private fun fcmPushNotificationProviderModule() = module {
    single<FcmPushNotificationProvider> {
        FcmPushNotificationProvider(
            config = get(),
            messengerConfig = get(),
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

private fun fcmPushNotificationProviderConfigModule(
    pushAppId: String,
    pushUrl: String,
    periodicSyncInterval: Duration
) = module {
    single { FcmPushNotificationProviderConfig(pushAppId, pushUrl, periodicSyncInterval) }
}

fun MatrixMultiMessengerConfiguration.addFcmPushNotificationProvider(
    pushAppId: String,
    pushUrl: String,
    periodicSyncInterval: Duration = 15.minutes,
) {
    modulesFactories += {
        fcmPushNotificationProviderConfigModule(pushAppId, pushUrl, periodicSyncInterval)
    }
    messengerConfiguration {
        addFcmPushNotificationProvider(
            pushAppId = pushAppId,
            pushUrl = pushUrl,
            periodicSyncInterval = periodicSyncInterval
        )
    }
}

fun MatrixMessengerConfiguration.addFcmPushNotificationProvider(
    pushAppId: String,
    pushUrl: String,
    periodicSyncInterval: Duration = 15.minutes,
) {
    modulesFactories += {
        fcmPushNotificationProviderConfigModule(pushAppId, pushUrl, periodicSyncInterval)
    }
    modulesFactories += ::fcmPushNotificationProviderModule
}
