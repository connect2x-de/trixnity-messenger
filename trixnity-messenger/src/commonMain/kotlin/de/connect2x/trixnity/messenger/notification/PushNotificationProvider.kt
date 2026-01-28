package de.connect2x.trixnity.messenger.notification

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.lognity.api.logger.warn
import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.MatrixMessengerAccountSettings
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerSettings
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.Worker
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerSettings
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerSettingsHolder
import de.connect2x.trixnity.messenger.settings.NestedSettingsView
import de.connect2x.trixnity.messenger.settings.SettingsView
import de.connect2x.trixnity.messenger.settings.settingsView
import de.connect2x.trixnity.messenger.update
import de.connect2x.trixnity.messenger.util.GetDefaultDeviceDisplayName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import de.connect2x.trixnity.client.notification
import de.connect2x.trixnity.clientserverapi.client.SyncState
import de.connect2x.trixnity.clientserverapi.model.push.PusherData
import de.connect2x.trixnity.clientserverapi.model.push.SetPushers
import de.connect2x.trixnity.core.MatrixServerException
import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.utils.retry

@Serializable
@NestedSettingsView("notification", "provider", "push")
data class MatrixMultiMessengerNotificationProviderPushSettings(
    val pushKey: String? = null,
) : SettingsView<MatrixMultiMessengerSettings>

val MatrixMultiMessengerSettings.notificationProviderPush
        by settingsView<MatrixMultiMessengerSettings, MatrixMultiMessengerNotificationProviderPushSettings>()

@Serializable
@NestedSettingsView("notification", "provider", "push")
data class MatrixMessengerNotificationProviderPushSettings(
    val pushKey: String? = null,
) : SettingsView<MatrixMessengerSettings>

val MatrixMessengerSettings.notificationProviderPush
        by settingsView<MatrixMessengerSettings, MatrixMessengerNotificationProviderPushSettings>()

@Serializable
@NestedSettingsView("notification", "provider", "push")
data class MatrixMessengerAccountNotificationProviderPushSettings(
    val enabled: Boolean = false,
    val deliveredPushKey: String? = null,
) : SettingsView<MatrixMessengerAccountSettings>

val MatrixMessengerAccountSettings.notificationProviderPush
        by settingsView<MatrixMessengerAccountSettings, MatrixMessengerAccountNotificationProviderPushSettings>()

/**
 * Basic implementation for push-based notifications.
 *
 * For it to work, you must implement and use [PushNotificationProviderPushKeyUpdater] too.
 */
abstract class PushNotificationProvider(
    private val config: MatrixMessengerConfiguration,
    private val multiSettings: MatrixMultiMessengerSettingsHolder?,
    private val settings: MatrixMessengerSettingsHolder,
    private val getDefaultDeviceDisplayName: GetDefaultDeviceDisplayName,
    private val matrixClients: MatrixClients,
    coroutineScope: CoroutineScope,
) : NotificationProvider, Worker {
    companion object {
        private val log: Logger = Logger("de.connect2x.trixnity.messenger.notification.PushNotificationProvider")
    }

    private val currentPushKey =
        (multiSettings?.map { it.notificationProviderPush.pushKey }
            ?: settings.map { it.notificationProviderPush.pushKey })
            .shareIn(coroutineScope, SharingStarted.Eagerly, replay = 1)

    override suspend fun doWork() {
        deliverPushKeys()
    }

    private suspend fun deliverPushKeys() {
        waitForStartup()
        while (currentCoroutineContext().isActive) {
            val currentPushKey = currentPushKey.filterNotNull().first()

            val accountsWithUndeliveredPushKey =
                settings.map {
                    it.base.accounts.filterValues { accountSettings ->
                        val notificationPush = accountSettings.notificationProviderPush
                        if (notificationPush.enabled) {
                            notificationPush.deliveredPushKey != currentPushKey
                        } else {
                            notificationPush.deliveredPushKey != null
                        }
                    }.mapValues { accountSettings ->
                        accountSettings.value.notificationProviderPush
                    }
                }.first { it.isNotEmpty() }

            val profile = multiSettings?.value?.base?.activeProfile
            coroutineScope {
                for ((account, currentSettings) in accountsWithUndeliveredPushKey) {
                    launch {
                        log.debug { "try set pusher for $account ($profile)" }
                        retry(
                            onError = { error, delay -> log.warn(error) { "could not set pusher for $account, retry again in $delay" } }
                        ) {
                            val changePusherResult =
                                if (currentSettings.enabled) {
                                    setPusher(profile, account, currentPushKey)
                                } else {
                                    removePusher(account, currentSettings.deliveredPushKey ?: return@retry)
                                }
                            changePusherResult.onFailure {
                                if (it is MatrixServerException) {
                                    log.warn(it) { "failed to deliver push key for $account" }
                                } else throw it
                            }
                            settings.update<MatrixMessengerAccountNotificationProviderPushSettings>(account) {
                                it.copy(deliveredPushKey = if (currentSettings.enabled) currentPushKey else null)
                            }
                        }
                        log.debug { "finished set pusher for $account ($profile)" }
                    }
                }
            }
        }
    }

    private val enabledForAccounts =
        settings.map { it.base.accounts.mapValues { it.value.notificationProviderPush.enabled } }
            .stateIn(
                coroutineScope,
                SharingStarted.Eagerly,
                settings.value.base.accounts.mapValues { it.value.notificationProviderPush.enabled }
            )


    override val canBeEnabled: Boolean = true
    override val isEnabled: StateFlow<Boolean> =
        enabledForAccounts.map { it.any { it.value } }
            .stateIn(
                coroutineScope,
                SharingStarted.Eagerly,
                enabledForAccounts.value.any { it.value }
            )

    override fun isEnabled(userId: UserId): Flow<Boolean> =
        enabledForAccounts.map { it[userId] ?: false }

    abstract suspend fun enableService()
    abstract suspend fun disableService()
    abstract suspend fun getPusherCustomFields(profile: String?, account: UserId): JsonObject?

    private val updateMutex = Mutex()

    /**
     * This should be called periodically to update existing notifications.
     */
    suspend fun possiblySyncAndProcessPending() {
        waitForStartup()
        updateMutex.withLock {
            log.debug { "possible sync and process pending notifications" }
            coroutineScope {
                matrixClients.value.values.forEach { matrixClient ->
                    if (!matrixClient.initialSyncDone.value) return@forEach
                    val syncState = matrixClient.syncState.value
                    if (syncState != SyncState.STOPPED && syncState != SyncState.ERROR) return@forEach
                    launch {
                        if (matrixClient.notification.getCount().first() != 0)
                            matrixClient.syncOnce().getOrNull()
                        matrixClient.notification.processPending()
                    }
                }
            }
            log.debug { "finished possible sync and process pending notifications" }
        }
    }

    /**
     * This should be called within a medial timeframe, when [onPush] return false.
     */
    suspend fun processPending(profile: String?, account: UserId?) {
        waitForStartup()
        log.debug { "process pending notifications" }
        when {
            profile != null && multiSettings?.value?.base?.activeProfile != profile -> return
            account != null -> matrixClients.value[account]?.notification?.processPending()
            else -> coroutineScope {
                matrixClients.value.values
                    .forEach { matrixClient ->
                        launch {
                            matrixClient.notification.processPending()
                        }
                    }
            }
        }
        log.debug { "finished process pending notifications" }
    }

    /**
     * As soon as this method is finished, the MatrixMessenger can be shut down again.
     */
    suspend fun onPush(profile: String?, account: UserId?, roomId: RoomId, eventId: EventId?): Boolean {
        waitForStartup()
        log.debug { "got push notification" }
        return when {
            multiSettings?.value?.base?.activeProfile != profile -> true
            account != null -> matrixClients.value[account]?.notification?.onPush(roomId, eventId) ?: true
            else -> matrixClients.value.values
                .map { matrixClient -> matrixClient.notification.onPush(roomId, eventId) }
                .any { it }
        }
    }

    override suspend fun enable(userId: UserId) {
        settings.update<MatrixMessengerAccountNotificationProviderPushSettings>(userId) {
            it.copy(enabled = true)
        }
        enableOrDisableService()
    }

    override suspend fun disable(userId: UserId) {
        settings.update<MatrixMessengerAccountNotificationProviderPushSettings>(userId) {
            it.copy(enabled = false)
        }
        enableOrDisableService()
    }

    private suspend fun enableOrDisableService() {
        val enabled = settings.value.base.accounts.values.any { account -> account.notificationProviderPush.enabled }
        if (enabled) {
            log.info { "enable push service" }
            enableService()
        } else {
            log.info { "disable push service" }
            disableService()
        }
    }

    private suspend fun setPusher(profile: String?, account: UserId, pushKey: String): Result<Unit> {
        val pushUrl = config.pushUrl
            ?: return Result.failure(IllegalStateException("cannot set pusher, because pushUrl is null"))
        val matrixClient = matrixClients.value[account]
            ?: return Result.failure(IllegalStateException("cannot set pusher, because MatrixClient not present"))
        return matrixClient.api.push.setPushers(
            SetPushers.Request.Set(
                appId = config.pushAppId ?: config.appId,
                appDisplayName = config.appName,
                data = PusherData(
                    url = pushUrl,
                    format = "event_id_only",
                    customFields = getPusherCustomFields(profile, account),
                ),
                deviceDisplayName = getDefaultDeviceDisplayName(),
                kind = "http",
                lang = "en",
                pushkey = pushKey,
                append = true, // ensure, that same pushKey can be used for multiple account
            )
        )
    }


    private suspend fun removePusher(userId: UserId, pushKey: String): Result<Unit> =
        matrixClients.value[userId]?.api?.push?.setPushers(
            SetPushers.Request.Remove(
                appId = config.pushAppId ?: config.appId,
                pushkey = pushKey,
            )
        ) ?: Result.success(Unit)

    private suspend fun waitForStartup() = matrixClients.isInitialized.first { it }
}
