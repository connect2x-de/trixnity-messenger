package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.messenger.MatrixMessengerAccountSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.update
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.util.getContentRules
import de.connect2x.trixnity.messenger.viewmodel.util.getServerDefaultRules
import de.connect2x.trixnity.messenger.viewmodel.util.toNotificationSettings
import de.connect2x.trixnity.messenger.viewmodel.util.toPushRuleSet
import io.github.oshai.kotlinlogging.KotlinLogging
import korlibs.io.async.launch
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.timeout
import net.folivo.trixnity.client.user
import net.folivo.trixnity.client.user.getAccountData
import net.folivo.trixnity.clientserverapi.model.push.SetPushRule
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.PushRulesEventContent
import org.koin.core.component.get
import org.koin.core.module.Module
import kotlin.time.Duration.Companion.seconds

private val log = KotlinLogging.logger { }

fun interface NotificationSettingsSingleAccountViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
    ): NotificationSettingsSingleAccountViewModel
}

data class NotificationSettings(
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
}

interface NotificationSettingsSingleAccountViewModelBase {
    val account: UserId

    val enabledForThisDevice: StateFlow<Boolean>
    fun toggleEnabledForThisDevice()

    val isUpdating: StateFlow<Boolean>
    val updateError: StateFlow<String?>

    val accountSettings: StateFlow<NotificationSettings>
    fun updateAccountSettings(settings: NotificationSettings)
}

/**
 * This interface may look different depending on the platform. Therefore, the UI should be platform dependent.
 */
expect interface NotificationSettingsSingleAccountViewModel : NotificationSettingsSingleAccountViewModelBase

class NotificationSettingsSingleAccountViewModelBaseImpl(
    viewModelContext: MatrixClientViewModelContext,
) : MatrixClientViewModelContext by viewModelContext, NotificationSettingsSingleAccountViewModelBase {
    override val account: UserId = userId
    private val i18n = get<I18n>()
    private val settings = get<MatrixMessengerSettingsHolder>()

    override val enabledForThisDevice: StateFlow<Boolean> = settings[userId]
        .map { it?.base?.notificationsEnabled == true }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), true)

    override fun toggleEnabledForThisDevice() {
        coroutineScope.launch {
            settings.update<MatrixMessengerAccountSettingsBase>(userId) {
                it.copy(notificationsEnabled = !it.notificationsEnabled)
            }
        }
    }

    override val isUpdating: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val updateError: MutableStateFlow<String?> = MutableStateFlow(null)

    override val accountSettings: StateFlow<NotificationSettings> =
        matrixClient.user.getAccountData<PushRulesEventContent>()
            .map { it?.global }
            .filterNotNull()
            .map { it.toNotificationSettings() }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), NotificationSettings())

    @OptIn(FlowPreview::class)
    override fun updateAccountSettings(settings: NotificationSettings) {
        if (isUpdating.getAndUpdate { true }.not()) {
            coroutineScope.launch {
                updateError.value = null

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
                        updateError.value = i18n.updateNotificationSettingsTimeoutError()
                    } else {
                        updateError.value = i18n.updateNotificationSettingsError(exception.message ?: "")
                    }
                }
            }.invokeOnCompletion { isUpdating.value = false }
        }
    }
}

expect fun platformNotificationSettingsSingleAccountViewModelFactoryModule(): Module
