package de.connect2x.trixnity.messenger.notification

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.lognity.api.logger.warn
import de.connect2x.trixnity.client.notification
import de.connect2x.trixnity.clientserverapi.client.SyncState
import de.connect2x.trixnity.clientserverapi.model.push.PusherData
import de.connect2x.trixnity.clientserverapi.model.push.SetPushers
import de.connect2x.trixnity.core.MatrixServerException
import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.m.Presence
import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.MatrixMessengerAccountSettings
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.Worker
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerSettingsHolder
import de.connect2x.trixnity.messenger.util.GetDefaultDeviceDisplayName
import de.connect2x.trixnity.utils.retry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Basic implementation for push-based notifications.
 */
abstract class PushNotificationProvider(
    private val pushAppId: String,
    private val config: MatrixMessengerConfiguration,
    protected val multiSettings: MatrixMultiMessengerSettingsHolder?,
    protected val settings: MatrixMessengerSettingsHolder,
    private val getDefaultDeviceDisplayName: GetDefaultDeviceDisplayName,
    private val matrixClients: MatrixClients,
    coroutineScope: CoroutineScope,
) : NotificationProvider, Worker {
    companion object {
        private val log: Logger = Logger("de.connect2x.trixnity.messenger.notification.PushNotificationProvider")
    }

    @Serializable
    data class PusherSettings(
        val pushKey: String,
        val url: String,
    )

    @Serializable
    data class PusherAccountSettings(
        val enabled: Boolean,
        val deliveredPusher: PusherSettings? = null,
    )

    abstract val currentPusherSettings: SharedFlow<PusherSettings?>
    abstract val MatrixMessengerAccountSettings.pusherSettings: PusherAccountSettings
    abstract suspend fun MatrixMessengerSettingsHolder.updatePusherSettings(
        account: UserId,
        updater: (PusherAccountSettings) -> PusherAccountSettings
    )

    override suspend fun doWork() {
        deliverPushKeys()
    }

    private suspend fun deliverPushKeys() {
        waitForStartup()
        while (currentCoroutineContext().isActive) {
            val accountsWithUndeliveredPushKey =
                combine(
                    settings,
                    currentPusherSettings.filterNotNull(),
                ) { settings, currentPusher ->
                    settings.base.accounts.filterValues { accountSettings ->
                        val notificationPush = accountSettings.pusherSettings
                        if (notificationPush.enabled) {
                            notificationPush.deliveredPusher != currentPusher
                        } else {
                            notificationPush.deliveredPusher != null
                        }
                    }.mapValues { accountSettings ->
                        accountSettings.value.pusherSettings to currentPusher
                    }
                }.first { it.isNotEmpty() }

            val profile = multiSettings?.value?.base?.activeProfile
            coroutineScope {
                for ((account, pusherSettings) in accountsWithUndeliveredPushKey) {
                    val (accountPusher, currentPusher) = pusherSettings
                    launch {
                        retry(
                            onError = { error, delay -> log.warn(error) { "could not set pusher for $account, retry again in $delay" } }
                        ) {
                            val changePusherResult =
                                if (accountPusher.enabled) {
                                    log.debug { "try set pusher for $account ($profile) from ${accountPusher.deliveredPusher} to $currentPusher" }
                                    if (accountPusher.deliveredPusher != null) {
                                        removePusher(account, accountPusher.deliveredPusher)
                                    }
                                    setPusher(profile, account, currentPusher)
                                } else {
                                    log.debug { "try remove pusher for $account ($profile)" }
                                    removePusher(account, accountPusher.deliveredPusher ?: return@retry)
                                }
                            changePusherResult.onFailure {
                                if (it is MatrixServerException) {
                                    log.warn(it) { "failed to set pusher for $account" }
                                } else throw it
                            }
                            settings.updatePusherSettings(account) {
                                it.copy(deliveredPusher = if (accountPusher.enabled) currentPusher else null)
                            }
                        }
                        log.debug { "finished set pusher for $account ($profile)" }
                    }
                }
            }
        }
    }

    private val enabledForAccounts =
        settings.map { it.base.accounts.mapValues { it.value.pusherSettings.enabled } }
            .stateIn(
                coroutineScope,
                SharingStarted.Eagerly,
                settings.value.base.accounts.mapValues { it.value.pusherSettings.enabled }
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
                        matrixClient.notification.processPending(Presence.ONLINE)
                    }
                }
            }
            log.debug { "finished possible sync and process pending notifications" }
        }
    }

    /**
     * This should be called within a medial timeframe, when [onPush] returned false.
     */
    suspend fun processPending(profile: String?, account: UserId?) {
        waitForStartup()
        log.debug { "process pending notifications" }
        when {
            profile != null && multiSettings?.value?.base?.activeProfile != profile -> return
            account != null -> matrixClients.value[account]?.notification?.processPending(Presence.OFFLINE)
            else -> coroutineScope {
                matrixClients.value.values
                    .forEach { matrixClient ->
                        launch {
                            matrixClient.notification.processPending(Presence.OFFLINE)
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
        settings.updatePusherSettings(userId) {
            it.copy(enabled = true)
        }
        enableOrDisableService()
    }

    override suspend fun disable(userId: UserId) {
        settings.updatePusherSettings(userId) {
            it.copy(enabled = false)
        }
        enableOrDisableService()
    }

    private suspend fun enableOrDisableService() {
        val enabled = settings.value.base.accounts.values.any { account -> account.pusherSettings.enabled }
        if (enabled) {
            log.info { "enable push service" }
            enableService()
        } else {
            log.info { "disable push service" }
            disableService()
        }
    }

    private suspend fun setPusher(profile: String?, account: UserId, pusher: PusherSettings): Result<Unit> {
        val matrixClient = matrixClients.value[account]
            ?: return Result.failure(IllegalStateException("cannot set pusher, because MatrixClient not present"))
        return matrixClient.api.push.setPushers(
            SetPushers.Request.Set(
                appId = pushAppId,
                appDisplayName = config.appName,
                data = PusherData(
                    url = pusher.url,
                    format = "event_id_only",
                    customFields = getPusherCustomFields(profile, account),
                ),
                deviceDisplayName = getDefaultDeviceDisplayName(),
                kind = "http",
                lang = "en",
                pushkey = pusher.pushKey,
                append = true, // ensure, that same pushKey can be used for multiple account
            )
        )
    }

    private suspend fun removePusher(userId: UserId, pusher: PusherSettings): Result<Unit> {
        return matrixClients.value[userId]?.api?.push?.setPushers(
            SetPushers.Request.Remove(
                appId = pushAppId,
                pushkey = pusher.pushKey,
            )
        ) ?: Result.success(Unit)
    }

    private suspend fun waitForStartup() = matrixClients.isInitialized.first { it }
}
