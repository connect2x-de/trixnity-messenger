package de.connect2x.trixnity.messenger.notification.apns

import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerConfiguration
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerSettingsHolder
import de.connect2x.trixnity.messenger.notification.NotificationProvider
import de.connect2x.trixnity.messenger.notification.NotificationProviders
import de.connect2x.trixnity.messenger.notification.PushNotificationProvider
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
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import platform.Foundation.NSData
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDelegateProtocol
import platform.UIKit.UIBackgroundFetchResult
import platform.darwin.NSObject

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

    override suspend fun enableService() {}

    override suspend fun disableService() {}

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

    open class UIApplicationDelegate : UIApplicationDelegateProtocol, NSObject() {
        override fun application(application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken: NSData) {
            val pushKey = didRegisterForRemoteNotificationsWithDeviceToken.toByteString().toHexString()
            runBlocking {
                withApnsPushNotificationProvider {
                    it.onPushKeyUpdate(pushKey)
                }
            }
        }

        override fun application(
            application: UIApplication,
            didReceiveRemoteNotification: Map<Any?, *>,
            fetchCompletionHandler: (UIBackgroundFetchResult) -> Unit
        ) {
            val profile = didReceiveRemoteNotification["profile"] as? String
            val account = (didReceiveRemoteNotification["account"] as? String)?.let(::UserId)
            val roomId = (didReceiveRemoteNotification["room_id"] as? String)?.let(::RoomId)
            val eventId = (didReceiveRemoteNotification["event_id"] as? String)?.let(::EventId)
            if (roomId != null) {
                runBlocking {
                    withApnsPushNotificationProvider {
                        it.onPush(profile, account, roomId, eventId)
                    }
                }
                if (!didAlreadyProcessOnPush) {
                    // FIXME enqueue process pending
                }
            }
            fetchCompletionHandler(UIBackgroundFetchResult.UIBackgroundFetchResultNoData)
        }
    }
}

internal suspend fun <T> withApnsPushNotificationProvider(
    block: suspend (ApnsPushNotificationProvider) -> T
): T = withMatrixMessengerFromService {
    val apnsPushNotificationProvider = it.di.get<NotificationProviders>()
        .first { it is ApnsPushNotificationProvider } as? ApnsPushNotificationProvider
    if (apnsPushNotificationProvider != null) block(apnsPushNotificationProvider)
    else throw IllegalStateException("ApnsPushNotificationProvider not found in DI")
}

fun MatrixMultiMessengerConfiguration.addApnsPushNotificationProvider() {
    modulesFactories += {
        module {
            single(named<ApnsPushNotificationProvider.UIApplicationDelegate>()) { ApnsPushNotificationProvider.UIApplicationDelegate() }
                .bind<UIApplicationDelegateProtocol>()
        }
    }
    messengerConfiguration {
        modulesFactories += {
            module {
                single<ApnsPushNotificationProvider>(named<ApnsPushNotificationProvider>()) {
                    ApnsPushNotificationProvider(
                        config = get(),
                        multiSettings = getOrNull(),
                        settings = get(),
                        getDefaultDeviceDisplayName = get(),
                        matrixClients = get(),
                        coroutineScope = get(),
                    )
                }.bind<NotificationProvider>()
            }
        }
    }
}

fun MatrixMessengerConfiguration.addApnsPushNotificationProvider() {
    modulesFactories += {
        module {
            single(named<ApnsPushNotificationProvider.UIApplicationDelegate>()) { ApnsPushNotificationProvider.UIApplicationDelegate() }
                .bind<UIApplicationDelegateProtocol>()
            single<ApnsPushNotificationProvider>(named<ApnsPushNotificationProvider>()) {
                ApnsPushNotificationProvider(
                    config = get(),
                    multiSettings = getOrNull(),
                    settings = get(),
                    getDefaultDeviceDisplayName = get(),
                    matrixClients = get(),
                    coroutineScope = get(),
                )
            }.bind<NotificationProvider>()
        }
    }
}
