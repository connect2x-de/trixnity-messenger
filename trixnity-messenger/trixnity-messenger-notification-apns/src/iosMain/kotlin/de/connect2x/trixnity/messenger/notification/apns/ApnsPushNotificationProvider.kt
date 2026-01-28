package de.connect2x.trixnity.messenger.notification.apns

import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.Worker
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerConfiguration
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerSettingsHolder
import de.connect2x.trixnity.messenger.notification.NotificationProvider
import de.connect2x.trixnity.messenger.notification.PushNotificationProvider
import de.connect2x.trixnity.messenger.notification.PushNotificationProviderPushKeyUpdater
import de.connect2x.trixnity.messenger.uikit.ApplicationDelegateProtocol
import de.connect2x.trixnity.messenger.uikit.WithDefault
import de.connect2x.trixnity.messenger.util.GetDefaultDeviceDisplayName
import de.connect2x.trixnity.messenger.withMatrixMessengerFromService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.io.bytestring.toByteString
import kotlinx.io.bytestring.toHexString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import platform.Foundation.NSData
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationLaunchOptionsKey
import platform.UIKit.UIBackgroundFetchResult

class ApnsPushNotificationProvider(
    config: MatrixMessengerConfiguration,
    multiSettings: MatrixMultiMessengerSettingsHolder?,
    settings: MatrixMessengerSettingsHolder,
    getDefaultDeviceDisplayName: GetDefaultDeviceDisplayName,
    matrixClients: MatrixClients,
    coroutineScope: CoroutineScope,
) : PushNotificationProvider(
    config = config,
    multiSettings = multiSettings,
    settings = settings,
    getDefaultDeviceDisplayName = getDefaultDeviceDisplayName,
    matrixClients = matrixClients,
    coroutineScope = coroutineScope,
) {
    companion object {
        const val ID = "de.connect2x.trixnity.messenger.notification.apns"
    }

    override val id: String = ID
    override val displayName: String = "Apple Push Notification service"

    override suspend fun enableService() {
        SyncAndProcessPendingWorker.enqueueUniquePeriodicWork()
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
        private val pushKeyUpdater: PushNotificationProviderPushKeyUpdater,
    ) : ApplicationDelegateProtocol {

        override fun didFinishLaunching(
            application: UIApplication, launchOptions: Map<UIApplicationLaunchOptionsKey, *>?
        ): WithDefault<Boolean> {
            SyncAndProcessPendingWorker.registerUniquePeriodicWork()

            return WithDefault.Default
        }

        override fun didRegisterForRemoteNotifications(application: UIApplication, deviceToken: NSData) {
            val pushKey = deviceToken.toByteString().toHexString()
            runBlocking { pushKeyUpdater.onPushKeyUpdate(pushKey) }
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

private fun apnsPushNotificationProviderPushKeyUpdaterModule() = module {
    single { PushNotificationProviderPushKeyUpdater(getOrNull(), getOrNull()) }
}

private fun apnsPushNotificationProviderUIApplicationDelegateModule() = module {
    single<ApplicationDelegateProtocol>(named<ApnsPushNotificationProvider.UIApplicationDelegate>()) {
        ApnsPushNotificationProvider.UIApplicationDelegate(get())
    }
}

fun MatrixMultiMessengerConfiguration.addApnsPushNotificationProvider() {
    modulesFactories += ::apnsPushNotificationProviderUIApplicationDelegateModule
    modulesFactories += ::apnsPushNotificationProviderPushKeyUpdaterModule
    messengerConfiguration {
        modulesFactories += ::apnsPushNotificationProviderModule
    }
}

fun MatrixMessengerConfiguration.addApnsPushNotificationProvider() {
    modulesFactories += ::apnsPushNotificationProviderUIApplicationDelegateModule
    modulesFactories += ::apnsPushNotificationProviderPushKeyUpdaterModule
    modulesFactories += ::apnsPushNotificationProviderModule
}
