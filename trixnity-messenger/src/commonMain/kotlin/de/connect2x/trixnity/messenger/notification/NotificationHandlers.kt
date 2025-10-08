package de.connect2x.trixnity.messenger.notification

import de.connect2x.sysnotify.NotificationHandler
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerSettingsHolder
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.updateAndGet
import net.folivo.trixnity.core.model.UserId
import org.koin.core.module.Module

private val log = KotlinLogging.logger("de.connect2x.trixnity.messenger.notification.NotificationHandlers")

@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
interface NotificationHandlers : AutoCloseable {
    /**
     * This should be called by the UI to continuously request permissions.
     */
    suspend fun continuouslyRequestPermissions()
    val global: NotificationHandler
    operator fun get(account: UserId): NotificationHandler
}

fun interface NotificationHandlerFactory {
    operator fun invoke(
        name: String,
        id: String,
        isDebugEnabled: Boolean,
        appId: String,
    ): NotificationHandler
}

@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
class NotificationHandlersImpl(
    private val config: MatrixMessengerConfiguration,
    private val notificationProviders: NotificationProviders,
    private val multiSettings: MatrixMultiMessengerSettingsHolder?,
    private val requestPermissionsCallback: (granted: Boolean) -> Unit = {},
    private val notificationHandlerFactory: NotificationHandlerFactory = NotificationHandlerFactory { name, id, isDebugEnabled, appId ->
        NotificationHandler(name = name, id = id, isDebugEnabled = isDebugEnabled, appId = appId)
    }
) : NotificationHandlers {

    override suspend fun continuouslyRequestPermissions() {
        combine(notificationProviders.map { it.isEnabled }) { it.any { it } }
            .distinctUntilChanged()
            .collect { anyNotificationsEnabled ->
                if (anyNotificationsEnabled && global.hasPermissions.not()) {
                    log.debug { "requesting notification permissions" }
                    global.requestPermissions { granted ->
                        if (!granted) {
                            log.warn { "notification permissions not granted" }
                        }
                        requestPermissionsCallback(granted)
                    }
                }
            }
    }

    private data class AccountInProfile(val profile: String?, val account: UserId) {
        val idSuffix = buildString {
            if (profile != null) {
                append("$profile-")
            }
            append(account.full)
        }
        val name = buildString {
            append(account.full)
            if (profile != null) {
                append(" ($profile)")
            }
        }
    }

    private val notificationHandlers: MutableStateFlow<Map<AccountInProfile, Lazy<NotificationHandler>>> =
        MutableStateFlow(mapOf())

    private fun notificationHandler(idSuffix: String, name: String): NotificationHandler =
        notificationHandlerFactory(
            id = "${config.appId}-$idSuffix",
            appId = config.appId,
            name = name,
            isDebugEnabled = config.isDebugEnabled,
        )

    private val _global = lazy {
        notificationHandler("global", config.appName)
    }
    override val global: NotificationHandler by lazy {
        notificationHandler("global", config.appName)
    }

    override operator fun get(account: UserId): NotificationHandler {
        val profile = multiSettings?.value?.base?.activeProfile
        val accountInProfile = AccountInProfile(profile, account)
        return checkNotNull(
            notificationHandlers.updateAndGet {
                if (it.contains(accountInProfile)) it
                else it + (accountInProfile to lazy {
                    notificationHandler(
                        accountInProfile.idSuffix,
                        accountInProfile.name
                    )
                })
            }[accountInProfile]
        ).value
    }

    override fun close() {
        if (_global.isInitialized()) global.close()
        notificationHandlers.value.values.forEach { it.value.close() }
    }
}

expect fun platformNotificationHandlersModule(): Module
