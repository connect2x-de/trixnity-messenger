package de.connect2x.trixnity.messenger.notification.unifiedpush

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
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
import de.connect2x.trixnity.messenger.util.ActivityGetter
import de.connect2x.trixnity.messenger.util.ContextGetter
import de.connect2x.trixnity.messenger.util.GetDefaultDeviceDisplayName
import de.connect2x.trixnity.messenger.withMatrixMessengerFromService
import io.github.oshai.kotlinlogging.KotlinLogging
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
import org.unifiedpush.android.connector.UnifiedPush
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

private val log = KotlinLogging.logger {}

@Serializable
@NestedSettingsView("notification", "provider", "unifiedpush")
data class MatrixMultiMessengerNotificationProviderUnifiedPushSettings(
    val pusher: PusherSettings? = null,
) : SettingsView<MatrixMultiMessengerSettings>

val MatrixMultiMessengerSettings.notificationProviderUnifiedPush
        by settingsView<MatrixMultiMessengerSettings, MatrixMultiMessengerNotificationProviderUnifiedPushSettings>()

@Serializable
@NestedSettingsView("notification", "provider", "unifiedpush")
data class MatrixMessengerNotificationProviderUnifiedPushSettings(
    val pusher: PusherSettings? = null,
) : SettingsView<MatrixMessengerSettings>

val MatrixMessengerSettings.notificationProviderUnifiedPush
        by settingsView<MatrixMessengerSettings, MatrixMessengerNotificationProviderUnifiedPushSettings>()

@Serializable
@NestedSettingsView("notification", "provider", "unifiedpush")
data class MatrixMessengerAccountNotificationProviderUnifiedPushSettings(
    val enabled: Boolean = false,
    val deliveredPusher: PusherSettings? = null,
) : SettingsView<MatrixMessengerAccountSettings>

val MatrixMessengerAccountSettings.notificationProviderUnifiedPush
        by settingsView<MatrixMessengerAccountSettings, MatrixMessengerAccountNotificationProviderUnifiedPushSettings>()


class UnifiedPushNotificationProvider(
    config: MatrixMessengerConfiguration,
    multiSettings: MatrixMultiMessengerSettingsHolder?,
    settings: MatrixMessengerSettingsHolder,
    getDefaultDeviceDisplayName: GetDefaultDeviceDisplayName,
    matrixClients: MatrixClients,
    coroutineScope: CoroutineScope,
    private val contextGetter: ContextGetter,
    private val activityGetter: ActivityGetter,
) : PushNotificationProvider(
    messengerConfig = config,
    multiSettings = multiSettings,
    settings = settings,
    getDefaultDeviceDisplayName = getDefaultDeviceDisplayName,
    matrixClients = matrixClients,
    coroutineScope = coroutineScope,
) {
    companion object Id : NotificationProvider.Id<UnifiedPushNotificationProvider>

    data class Config(
        override val appId: String,
        val url: String,
        val periodicSyncInterval: Duration,
    ) : PushNotificationProvider.Config

    override val id = Id
    override val config = getProviderConfig<Config>()
    override val displayName: String = "UnifiedPush"

    override val canBeEnabled: Boolean get() = UnifiedPush.getDistributors(contextGetter()).isNotEmpty()

    override val currentPusherSettings =
        (multiSettings?.map { s -> s.notificationProviderUnifiedPush.pusher }
            ?: settings.map { s -> s.notificationProviderUnifiedPush.pusher })
            .shareIn(coroutineScope, SharingStarted.Eagerly, replay = 1)
    override val MatrixMessengerAccountSettings.pusherSettings: PusherAccountSettings
        get() = notificationProviderUnifiedPush.run {
            PusherAccountSettings(
                enabled = enabled,
                deliveredPusher = deliveredPusher,
            )
        }

    override suspend fun MatrixMessengerSettingsHolder.updatePusherSettings(
        account: UserId,
        updater: (PusherAccountSettings) -> PusherAccountSettings
    ) {
        update<MatrixMessengerAccountNotificationProviderUnifiedPushSettings>(account) {
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
        val activity = activityGetter()
        activity.packageManager.setComponentEnabledSetting(
            ComponentName(activity, TrixnityMessengerUnifiedPushService::class.java),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP,
        )
        SyncAndProcessPendingWorker.enqueueUniquePeriodicWork(
            context = activity,
            interval = config.periodicSyncInterval
        )
        UnifiedPush.tryUseCurrentOrDefaultDistributor(context = activity) { success ->
            if (!success) {
                log.error { "Could not initialize UnifiedPush (distributors=${UnifiedPush.getDistributors(activity)})" }
            } else {
                UnifiedPush.register(context = activity)
            }
        }
    }

    override suspend fun disableService() {
        val activity = activityGetter()
        activity.packageManager.setComponentEnabledSetting(
            ComponentName(activity, TrixnityMessengerUnifiedPushService::class.java),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
        )
        SyncAndProcessPendingWorker.stopUniquePeriodicWork(context = activity)
        UnifiedPush.unregister(context = activity)
    }

    override suspend fun getPusherCustomFields(profile: String?, account: UserId): JsonObject =
        buildJsonObject {
            putJsonObject("default_payload") {
                profile?.let { put("profile", it) }
                put("account", account.full)
            }
        }
}

internal suspend fun <T> withUnifiedPushNotificationProvider(
    context: Context,
    block: suspend (UnifiedPushNotificationProvider) -> T
): T = withMatrixMessengerFromService(context) {
    val unifiedPushNotificationProvider = it.di.get<UnifiedPushNotificationProvider>()
    block(unifiedPushNotificationProvider)
}

private fun unifiedPushNotificationProviderModule() = module {
    single<UnifiedPushNotificationProvider> {
        UnifiedPushNotificationProvider(
            config = get(),
            multiSettings = getOrNull(),
            settings = get(),
            getDefaultDeviceDisplayName = get(),
            matrixClients = get(),
            coroutineScope = get(),
            contextGetter = get(),
            activityGetter = get(),
        )
    }.apply {
        bind<NotificationProvider>()
        bind<Worker>()
    }
}

fun MatrixMultiMessengerConfiguration.addUnifiedPushNotificationProvider(
    pushAppId: String,
    pushUrl: String,
    periodicSyncInterval: Duration = 15.minutes,
) {
    messengerConfiguration {
        modulesFactories += ::unifiedPushNotificationProviderModule
        notificationProviderConfigurations[UnifiedPushNotificationProvider] =
            UnifiedPushNotificationProvider.Config(
                appId = pushAppId,
                url = pushUrl,
                periodicSyncInterval = periodicSyncInterval
            )
    }
}

fun MatrixMessengerConfiguration.addUnifiedPushNotificationProvider(
    pushAppId: String,
    pushUrl: String,
    periodicSyncInterval: Duration = 15.minutes,
) {
    modulesFactories += ::unifiedPushNotificationProviderModule
    notificationProviderConfigurations[UnifiedPushNotificationProvider] =
        UnifiedPushNotificationProvider.Config(
            appId = pushAppId,
            url = pushUrl,
            periodicSyncInterval = periodicSyncInterval
        )
}
