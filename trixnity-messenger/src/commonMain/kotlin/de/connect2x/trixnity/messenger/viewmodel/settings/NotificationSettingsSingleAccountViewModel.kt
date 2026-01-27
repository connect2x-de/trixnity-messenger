package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.messenger.MatrixMessengerAccountNotificationSettings
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.notification.NoOpNotificationProvider
import de.connect2x.trixnity.messenger.notification.NotificationHandlers
import de.connect2x.trixnity.messenger.notification.NotificationProviders
import de.connect2x.trixnity.messenger.update
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.settings.NotificationSettingsSingleAccountViewModel.NotificationProviderViewModel
import de.connect2x.trixnity.messenger.viewmodel.util.getContentRules
import de.connect2x.trixnity.messenger.viewmodel.util.getServerDefaultRules
import de.connect2x.trixnity.messenger.viewmodel.util.toNotificationSettings
import de.connect2x.trixnity.messenger.viewmodel.util.toPushRuleSet
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.folivo.trixnity.client.user
import net.folivo.trixnity.client.user.getAccountData
import net.folivo.trixnity.clientserverapi.model.push.SetPushRule
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.PushRulesEventContent
import org.koin.core.component.get
import kotlin.time.Duration.Companion.seconds

private val log = KotlinLogging.logger { }

interface NotificationSettingsSingleAccountViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
    ): NotificationSettingsSingleAccountViewModel =
        NotificationSettingsSingleAccountViewModelImpl(viewModelContext)

    companion object : NotificationSettingsSingleAccountViewModelFactory
}

data class AccountNotificationSettings(
    val defaultLevel: DefaultLevel = DefaultLevel.ROOM,
    val sound: Sound = Sound(),
    val activity: Activity = Activity(),
    val mention: Mention = Mention(),
    val keywords: Set<String> = setOf()
) {
    enum class DefaultLevel {
        NONE, MENTION, DM, ROOM;
    }

    data class Sound(
        val room: Boolean = true,
        val dm: Boolean = true,
        val mention: Boolean = true,
        val call: Boolean = true,
    )

    data class Activity(
        val invite: Boolean = true,
        val status: Boolean = false,
        val notice: Boolean = false,
    )

    data class Mention(
        val user: Boolean = true,
        val room: Boolean = true,
        val keyword: Boolean = true,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as AccountNotificationSettings

        if (defaultLevel != other.defaultLevel) return false
        if (sound != other.sound) return false
        if (activity != other.activity) return false
        val unifiedMention =
            if (keywords.isEmpty() && mention.keyword) mention.copy(keyword = false)
            else mention
        val unifiedOtherMention =
            if (other.keywords.isEmpty() && other.mention.keyword) other.mention.copy(keyword = false)
            else other.mention
        if (unifiedMention != unifiedOtherMention) return false
        if (keywords != other.keywords) return false

        return true
    }
}

data class DeviceNotificationSettings(
    val playSound: Boolean = true,
    val showDetails: Boolean = true,
)

interface NotificationSettingsSingleAccountViewModel {
    data class NotificationProviderViewModel(
        val id: String,
        val displayName: String,
    )

    val account: UserId

    val enabledForThisDevice: StateFlow<Boolean>
    fun toggleEnabledForThisDevice()
    val availableProviders: List<NotificationProviderViewModel>
    val selectedProvider: StateFlow<NotificationProviderViewModel?>
    fun selectProvider(id: String)

    val notificationHandlerId: String
    val notificationPermissionsNecessary: StateFlow<Boolean>

    val deviceSettings: StateFlow<DeviceNotificationSettings>
    fun updateDeviceSettings(settings: DeviceNotificationSettings)

    val accountSettings: StateFlow<AccountNotificationSettings>
    fun updateAccountSettings(settings: AccountNotificationSettings)
    val accountSettingsIsUpdating: StateFlow<Boolean>
    val updateAccountSettingsError: StateFlow<String?>
}

class NotificationSettingsSingleAccountViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
) : MatrixClientViewModelContext by viewModelContext, NotificationSettingsSingleAccountViewModel {
    override val account: UserId = userId
    private val i18n = get<I18n>()
    private val notificationProviders = get<NotificationProviders>()
    private val notificationHandlers = get<NotificationHandlers>()

    private val settingsHolder = get<MatrixMessengerSettingsHolder>()

    override val enabledForThisDevice: StateFlow<Boolean> =
        combine(notificationProviders.map { it.isEnabled(account) }) { it.toList().any { it } }
            .stateIn(coroutineScope, Eagerly, false)

    override val availableProviders: List<NotificationProviderViewModel> =
        notificationProviders.filter { it.canBeEnabled && it !is NoOpNotificationProvider }
            .map { NotificationProviderViewModel(it.id, it.displayName) }

    private val changeProviderMutex = Mutex()
    override fun toggleEnabledForThisDevice() {
        coroutineScope.launch {
            changeProviderMutex.withLock {
                if (enabledForThisDevice.value) {
                    notificationProviders.forEach { it.disable(account) }
                } else {
                    notificationProviders.firstOrNull { it.canBeEnabled }
                        ?.enable(account)
                }
            }
        }
    }

    override val selectedProvider: StateFlow<NotificationProviderViewModel?> =
        combine(notificationProviders.map { notificationProvider ->
            notificationProvider.isEnabled(account).map { notificationProvider to it }
        }
        ) { it }
            .filterNot { it.none { (_, enabled) -> enabled } } // prevent flickering when selecting a new provider
            .map { notificationProvidersEnabled ->
                val firstEnabledNotificationProvider =
                    notificationProvidersEnabled
                        .firstOrNull { it.first !is NoOpNotificationProvider && it.second }
                        ?.first
                        ?: return@map null
                NotificationProviderViewModel(
                    firstEnabledNotificationProvider.id,
                    firstEnabledNotificationProvider.displayName
                )
            }.stateIn(coroutineScope, WhileSubscribed(), null)

    override fun selectProvider(id: String) {
        coroutineScope.launch {
            log.debug { "select notification provider $id" }
            changeProviderMutex.withLock {
                notificationProviders.filter { it.id != id }.forEach { it.disable(account) }
                notificationProviders.find { it.id == id }?.enable(account)
            }
        }
    }

    override val notificationHandlerId: String = notificationHandlers[account].id

    @OptIn(ExperimentalCoroutinesApi::class)
    override val notificationPermissionsNecessary: StateFlow<Boolean> =
        enabledForThisDevice.transformLatest { enabled ->
            if (enabled.not()) emit(false)
            else {
                while (currentCoroutineContext().isActive) {
                    val hasPermissions = notificationHandlers.global.hasPermissions
                    delay(1.seconds)
                    emit(hasPermissions.not())
                }
            }
        }.stateIn(coroutineScope, WhileSubscribed(), false)

    override val deviceSettings: StateFlow<DeviceNotificationSettings> =
        settingsHolder[account].filterNotNull().map { it.notification }
            .map {
                DeviceNotificationSettings(
                    playSound = it.playSound,
                    showDetails = it.showDetails,
                )
            }
            .stateIn(coroutineScope, WhileSubscribed(), DeviceNotificationSettings())

    override fun updateDeviceSettings(settings: DeviceNotificationSettings) {
        coroutineScope.launch {
            settingsHolder.update<MatrixMessengerAccountNotificationSettings>(account) {
                MatrixMessengerAccountNotificationSettings(
                    playSound = settings.playSound,
                    showDetails = settings.showDetails,
                )
            }
        }
    }

    override val accountSettingsIsUpdating: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val updateAccountSettingsError: MutableStateFlow<String?> = MutableStateFlow(null)

    override val accountSettings: StateFlow<AccountNotificationSettings> =
        matrixClient.user.getAccountData<PushRulesEventContent>()
            .map { it?.global }
            .filterNotNull()
            .map { it.toNotificationSettings() }
            .stateIn(coroutineScope, WhileSubscribed(), AccountNotificationSettings())

    @OptIn(FlowPreview::class)
    override fun updateAccountSettings(settings: AccountNotificationSettings) {
        if (accountSettingsIsUpdating.getAndUpdate { true }.not()) {
            coroutineScope.launch {
                updateAccountSettingsError.value = null

                val currentPushRuleSet =
                    matrixClient.user.getAccountData<PushRulesEventContent>()
                        .map { it?.global }
                        .first()
                if (currentPushRuleSet?.toNotificationSettings() == settings) {
                    log.debug { "no change in settings" }
                    return@launch
                }
                val newPushRuleSet = settings.toPushRuleSet(userId)

                val currentServerDefaultRules = currentPushRuleSet?.getServerDefaultRules().orEmpty()
                val currentContentRules = currentPushRuleSet?.getContentRules().orEmpty()

                val newServerDefaultRules = newPushRuleSet.getServerDefaultRules()
                val newContentRules = newPushRuleSet.getContentRules()

                val updatedServerDefaultRules =
                    newServerDefaultRules.values.toSet() - currentServerDefaultRules.values.toSet()
                val updatedContentRules = newContentRules.values.toSet() - currentContentRules.values.toSet()
                val deletedContentRules = currentContentRules - newContentRules.keys

                log.debug { "update push rules" }
                try {
                    coroutineScope {
                        updatedServerDefaultRules.forEach { rule ->
                            if (rule.enabled != currentServerDefaultRules[rule.ruleId]?.enabled) {
                                log.trace { "set enabled of push rule ${rule.ruleId} to ${rule.enabled}" }
                                launch {
                                    matrixClient.api.push.setPushRuleEnabled(
                                        scope = "global",
                                        kind = rule.kind,
                                        ruleId = rule.ruleId,
                                        enabled = rule.enabled,
                                    ).getOrThrow()
                                }
                            }
                            if (rule.actions != currentServerDefaultRules[rule.ruleId]?.actions)
                                launch {
                                    log.trace { "set actions of push rule ${rule.ruleId} to ${rule.actions}" }
                                    matrixClient.api.push.setPushRuleActions(
                                        scope = "global",
                                        kind = rule.kind,
                                        ruleId = rule.ruleId,
                                        actions = rule.actions,
                                    ).getOrThrow()
                                }
                        }
                        updatedContentRules.forEach { rule ->
                            launch {
                                log.trace { "add content push rule ${rule.ruleId}" }
                                matrixClient.api.push.setPushRule(
                                    scope = "global",
                                    kind = rule.kind,
                                    ruleId = rule.ruleId,
                                    pushRule = SetPushRule.Request(
                                        actions = rule.actions,
                                        pattern = rule.pattern,
                                    )
                                ).getOrThrow()
                            }
                        }
                        deletedContentRules.values.forEach { rule ->
                            launch {
                                log.trace { "delete content push rule ${rule.ruleId}" }
                                matrixClient.api.push.deletePushRule(
                                    scope = "global",
                                    kind = rule.kind,
                                    ruleId = rule.ruleId,
                                ).getOrThrow()
                            }
                        }
                    }
                    matrixClient.user.getAccountData<PushRulesEventContent>()
                        .map { it?.global }
                        .timeout(10.seconds)
                        .first {
                            it?.toNotificationSettings() == settings
                        }
                } catch (exception: Exception) {
                    log.warn(exception) { "there was an error updating the notification settings" }
                    if (exception is TimeoutCancellationException) {
                        updateAccountSettingsError.value = i18n.updateNotificationSettingsTimeoutError()
                    } else {
                        updateAccountSettingsError.value = i18n.updateNotificationSettingsError(exception.message ?: "")
                    }
                }
            }.invokeOnCompletion { accountSettingsIsUpdating.value = false }
        }
    }
}
