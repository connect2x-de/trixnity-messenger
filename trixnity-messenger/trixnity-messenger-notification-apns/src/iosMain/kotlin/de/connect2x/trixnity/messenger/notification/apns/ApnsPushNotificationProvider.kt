package de.connect2x.trixnity.messenger.notification.apns

import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.MatrixMessengerAccountSettings
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerSettings
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.Worker
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerConfiguration
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerSettings
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerSettingsHolder
import de.connect2x.trixnity.messenger.multi.update
import de.connect2x.trixnity.messenger.notification.NotificationProvider
import de.connect2x.trixnity.messenger.notification.PushNotificationProvider
import de.connect2x.trixnity.messenger.notification.PushNotificationProvider.PusherSettings
import de.connect2x.trixnity.messenger.settings.NestedSettingsView
import de.connect2x.trixnity.messenger.settings.SettingsView
import de.connect2x.trixnity.messenger.settings.settingsView
import de.connect2x.trixnity.messenger.uikit.ApplicationDelegateProtocol
import de.connect2x.trixnity.messenger.uikit.WithDefault
import de.connect2x.trixnity.messenger.update
import de.connect2x.trixnity.messenger.util.GetDefaultDeviceDisplayName
import de.connect2x.trixnity.messenger.withMatrixMessengerFromService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.runBlocking
import kotlinx.io.bytestring.toByteString
import kotlinx.io.bytestring.toHexString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import platform.Foundation.NSData
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationLaunchOptionsKey
import platform.UIKit.UIBackgroundFetchResult
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

@Serializable
@NestedSettingsView("notification", "provider", "apns")
data class MatrixMultiMessengerNotificationProviderApnsSettings(
    val pusher: PusherSettings? = null,
) : SettingsView<MatrixMultiMessengerSettings>

val MatrixMultiMessengerSettings.notificationProviderApns
        by settingsView<MatrixMultiMessengerSettings, MatrixMultiMessengerNotificationProviderApnsSettings>()

@Serializable
@NestedSettingsView("notification", "provider", "apns")
data class MatrixMessengerNotificationProviderApnsSettings(
    val pusher: PusherSettings? = null,
) : SettingsView<MatrixMessengerSettings>

val MatrixMessengerSettings.notificationProviderApns
        by settingsView<MatrixMessengerSettings, MatrixMessengerNotificationProviderApnsSettings>()

@Serializable
@NestedSettingsView("notification", "provider", "apns")
data class MatrixMessengerAccountNotificationProviderApnsSettings(
    val enabled: Boolean = false,
    val deliveredPusher: PusherSettings? = null,
) : SettingsView<MatrixMessengerAccountSettings>

val MatrixMessengerAccountSettings.notificationProviderApns
        by settingsView<MatrixMessengerAccountSettings, MatrixMessengerAccountNotificationProviderApnsSettings>()

class ApnsPushNotificationProvider(
    config: MatrixMessengerConfiguration,
    multiSettings: MatrixMultiMessengerSettingsHolder?,
    settings: MatrixMessengerSettingsHolder,
    getDefaultDeviceDisplayName: GetDefaultDeviceDisplayName,
    matrixClients: MatrixClients,
    coroutineScope: CoroutineScope,
) : PushNotificationProvider(
    messengerConfig = config,
    multiSettings = multiSettings,
    settings = settings,
    getDefaultDeviceDisplayName = getDefaultDeviceDisplayName,
    matrixClients = matrixClients,
    coroutineScope = coroutineScope,
) {
    companion object Id : NotificationProvider.Id<ApnsPushNotificationProvider>

    data class Config(
        override val appId: String,
        val url: String,
        val periodicSyncInterval: Duration,
    ) : PushNotificationProvider.Config

    override val id = Id
    override val config = getProviderConfig<Config>()
    override val displayName: String = "Apple Push Notification service"

    override val currentPusherSettings =
        (multiSettings?.map { s -> s.notificationProviderApns.pusher }
            ?: settings.map { s -> s.notificationProviderApns.pusher })
            .shareIn(coroutineScope, SharingStarted.Eagerly, replay = 1)
    override val MatrixMessengerAccountSettings.pusherSettings: PusherAccountSettings
        get() = notificationProviderApns.run {
            PusherAccountSettings(
                enabled = enabled,
                deliveredPusher = deliveredPusher,
            )
        }

    override suspend fun MatrixMessengerSettingsHolder.updatePusherSettings(
        account: UserId,
        updater: (PusherAccountSettings) -> PusherAccountSettings
    ) {
        update<MatrixMessengerAccountNotificationProviderApnsSettings>(account) {
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
        SyncAndProcessPendingWorker.enqueueUniquePeriodicWork(getProviderConfig<Config>().periodicSyncInterval)
    }

    override suspend fun disableService() {
        SyncAndProcessPendingWorker.stopUniquePeriodicWork()
    }

    override suspend fun getPusherCustomFields(
        profile: String?,
        account: UserId,
    ): JsonObject =
        buildJsonObject {
            putJsonObject("default_payload") {
                profile?.let { put("profile", it) }
                put("account", account.full)
                putJsonObject("aps") {
                    put("content-available", 1)
                }
            }
        }

    class UIApplicationDelegate(
        private val multiSettings: MatrixMultiMessengerSettingsHolder?,
        private val settings: MatrixMessengerSettingsHolder,
        private val config: MatrixMessengerConfiguration,
    ) : ApplicationDelegateProtocol {

        override fun didFinishLaunching(
            application: UIApplication, launchOptions: Map<UIApplicationLaunchOptionsKey, *>?
        ): WithDefault<Boolean> {
            SyncAndProcessPendingWorker.registerUniquePeriodicWork()

            return WithDefault.Default
        }

        override fun didRegisterForRemoteNotifications(application: UIApplication, deviceToken: NSData) {
            val url =
                (config.notificationProviderConfigurations[ApnsPushNotificationProvider] as? Config
                    ?: throw IllegalStateException("cannot set pusher, because notificationProviderConfigurations[ApnsPushNotificationProvider.Id] is not of type ApnsPushNotificationProvider.Config")
                        ).url
            val pushKey = deviceToken.toByteString().toHexString()
            val pusher = PusherSettings(pushKey = pushKey, url = url)
            runBlocking {
                if (multiSettings != null) {
                    multiSettings.update<MatrixMultiMessengerNotificationProviderApnsSettings> {
                        it.copy(pusher = pusher)
                    }
                } else if (settings != null) {
                    settings.update<MatrixMessengerNotificationProviderApnsSettings> {
                        it.copy(pusher = pusher)
                    }
                }
            }
        }

        override suspend fun didReceiveRemoteNotification(
            application: UIApplication, userInfo: Map<Any?, *>
        ): WithDefault<UIBackgroundFetchResult> {
            val profile = userInfo["profile"] as? String
            val account = (userInfo["account"] as? String)?.let(::UserId)
            val roomId = (userInfo["room_id"] as? String)?.let(::RoomId)
            val eventId = (userInfo["event_id"] as? String)?.let(::EventId)

            if (roomId == null) {
                return WithDefault.Value(UIBackgroundFetchResult.UIBackgroundFetchResultNoData)
            }

            return withApnsPushNotificationProvider {
                val didAlreadyProcessOnPush = it.onPush(profile, account, roomId, eventId)

                if (didAlreadyProcessOnPush) {
                    WithDefault.Value(UIBackgroundFetchResult.UIBackgroundFetchResultNoData)
                } else {
                    it.processPending(profile, account)
                    WithDefault.Value(UIBackgroundFetchResult.UIBackgroundFetchResultNewData)
                }
            }
        }
    }
}

internal suspend fun <T> withApnsPushNotificationProvider(
    block: suspend (ApnsPushNotificationProvider) -> T
): T = withMatrixMessengerFromService {
    val apnsPushNotificationProvider = it.di.get<ApnsPushNotificationProvider>()
    block(apnsPushNotificationProvider)
}

private fun apnsPushNotificationProviderModule() = module {
    single<ApnsPushNotificationProvider> {
        ApnsPushNotificationProvider(
            config = get(),
            multiSettings = getOrNull(),
            settings = get(),
            getDefaultDeviceDisplayName = get(),
            matrixClients = get(),
            coroutineScope = get(),
        )
    }.apply {
        bind<NotificationProvider>()
        bind<Worker>()
    }
}

private fun apnsPushNotificationProviderUIApplicationDelegateModule() = module {
    single<ApplicationDelegateProtocol>(named<ApnsPushNotificationProvider.UIApplicationDelegate>()) {
        ApnsPushNotificationProvider.UIApplicationDelegate(get(), get(), get())
    }
}

fun MatrixMultiMessengerConfiguration.addApnsPushNotificationProvider(
    pushAppId: String,
    pushUrl: String,
    periodicSyncInterval: Duration = 15.minutes,
) {
    modulesFactories += ::apnsPushNotificationProviderUIApplicationDelegateModule
    messengerConfiguration {
        modulesFactories += ::apnsPushNotificationProviderModule
        notificationProviderConfigurations[ApnsPushNotificationProvider] =
            ApnsPushNotificationProvider.Config(
                appId = pushAppId,
                url = pushUrl,
                periodicSyncInterval = periodicSyncInterval
            )
    }
}

fun MatrixMessengerConfiguration.addApnsPushNotificationProvider(
    pushAppId: String,
    pushUrl: String,
    periodicSyncInterval: Duration = 15.minutes,
) {
    modulesFactories += ::apnsPushNotificationProviderUIApplicationDelegateModule
    modulesFactories += ::apnsPushNotificationProviderModule
    notificationProviderConfigurations[ApnsPushNotificationProvider] =
        ApnsPushNotificationProvider.Config(
            appId = pushAppId,
            url = pushUrl,
            periodicSyncInterval = periodicSyncInterval
        )
}
