package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.folivo.trixnity.client.user
import net.folivo.trixnity.client.user.getAccountData
import net.folivo.trixnity.core.model.events.m.PushRulesEventContent
import net.folivo.trixnity.core.model.push.PushAction
import net.folivo.trixnity.core.model.push.PushRule
import net.folivo.trixnity.core.model.push.PushRuleSet
import net.folivo.trixnity.core.model.push.ServerDefaultPushRules
import org.koin.core.module.Module


fun interface NotificationSettingsSingleAccountViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
    ): NotificationSettingsSingleAccountViewModel
}

data class NotificationSettings(
    val defaultLevel: DefaultLevel = DefaultLevel.ALL,
    val sound: Sound = Sound(),
    val activity: Activity = Activity(),
    val mention: Mention = Mention(),
    val keywords: Set<String> = setOf()
) {
    enum class DefaultLevel {
        NONE, MENTION, DM, ALL;
    }

    data class Sound(
        val dm: String? = "default",
        val mention: String? = "default",
        val call: String? = "ring",
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
    val isUpdating: StateFlow<Boolean>
    val updateError: StateFlow<String?>

    val settings: StateFlow<NotificationSettings>
}

/**
 * This interface may look different depending on the platform. Therefore, the UI should be platform dependent.
 */
expect interface NotificationSettingsSingleAccountViewModel : NotificationSettingsSingleAccountViewModelBase

class NotificationSettingsSingleAccountViewModelBaseImpl(
    viewModelContext: MatrixClientViewModelContext,
) : MatrixClientViewModelContext by viewModelContext, NotificationSettingsSingleAccountViewModelBase {
    override val isUpdating: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val updateError: MutableStateFlow<String?> = MutableStateFlow(null)

    override val settings: StateFlow<NotificationSettings> =
        matrixClient.user.getAccountData<PushRulesEventContent>()
            .map { it?.global }
            .filterNotNull()
            .map { it.toNotificationSettings() }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), NotificationSettings())

    private fun PushRuleSet.toNotificationSettings(): NotificationSettings {
        val pushRules = (override.orEmpty() +
                content.orEmpty() +
                room.orEmpty() +
                sender.orEmpty() +
                underride.orEmpty())
            .filter { it.enabled }
            .associateBy { it.ruleId }
        val dmRules = setOfNotNull(
            pushRules[ServerDefaultPushRules.RoomOneToOne.id],
            pushRules[ServerDefaultPushRules.EncryptedRoomOneToOne.id]
        )
        val roomRules = setOfNotNull(
            pushRules[ServerDefaultPushRules.Message.id],
            pushRules[ServerDefaultPushRules.Encrypted.id]
        )
        val contentRules = content.orEmpty().filterNot { it.ruleId.startsWith(".") }

        return NotificationSettings(
            defaultLevel = when {
                pushRules[ServerDefaultPushRules.Master.id]?.enabled == false -> NotificationSettings.DefaultLevel.NONE
                roomRules.shouldNotify() -> NotificationSettings.DefaultLevel.ALL
                dmRules.shouldNotify() -> NotificationSettings.DefaultLevel.DM
                else -> NotificationSettings.DefaultLevel.MENTION
            },
            sound = NotificationSettings.Sound(
                dm = dmRules.shouldSetSoundTweak(),
                mention = (setOfNotNull(
                    pushRules[ServerDefaultPushRules.IsUserMention.id],
                    pushRules[ServerDefaultPushRules.IsRoomMention.id]
                ) + contentRules).shouldSetSoundTweak(),
                call = setOfNotNull(pushRules[ServerDefaultPushRules.Call.id]).shouldSetSoundTweak(),
            ),
            activity = NotificationSettings.Activity(
                invite = setOfNotNull(pushRules[ServerDefaultPushRules.InviteForMe.id]).shouldNotify(),
                status = setOfNotNull(
                    pushRules[ServerDefaultPushRules.MemberEvent.id],
                    pushRules[ServerDefaultPushRules.Tombstone.id]
                ).shouldNotify(),
                notice = setOfNotNull(pushRules[ServerDefaultPushRules.SuppressNotice.id]).shouldMute().not(),
            ),
            mention = NotificationSettings.Mention(
                user = setOfNotNull(pushRules[ServerDefaultPushRules.IsUserMention.id]).shouldNotify(),
                room = setOfNotNull(pushRules[ServerDefaultPushRules.IsRoomMention.id]).shouldNotify(),
                keyword = contentRules.shouldNotify(),
            ),
            keywords = contentRules.map { it.pattern }.toSet()
        )
    }

    private fun Collection<PushRule>.shouldNotify(): Boolean = any { it.actions.contains(PushAction.Notify) }

    private fun Collection<PushRule>.shouldSetSoundTweak(): String? =
        filter { it.actions.contains(PushAction.Notify) }
            .firstNotNullOfOrNull { pushRule ->
                pushRule.actions.filterIsInstance<PushAction.SetSoundTweak>().firstOrNull()
            }
            ?.value

    private fun Collection<PushRule>.shouldMute(): Boolean =
        any {
            !it.actions.contains(PushAction.Notify) &&
                    it.actions.filterIsInstance<PushAction.SetSoundTweak>().firstOrNull()?.value.isNullOrEmpty() &&
                    it.actions.filterIsInstance<PushAction.SetHighlightTweak>().firstOrNull()?.value != true
        }

    private fun NotificationSettings.toPushRuleSet(): PushRuleSet {
        PushRuleSet(
            override = listOf(
                ServerDefaultPushRules.Master.rule.copy(enabled = defaultLevel != NotificationSettings.DefaultLevel.NONE),
                ServerDefaultPushRules.SuppressNotice.rule.copy(enabled = !activity.notice),
                ServerDefaultPushRules.InviteForMe(userId).rule.copy(enabled = activity.invite),
                ServerDefaultPushRules.MemberEvent.rule.copy(enabled = activity.status),
                ServerDefaultPushRules.Tombstone.rule.copy(enabled = activity.status),
                ServerDefaultPushRules.IsUserMention(userId).rule.copy(enabled = mention.user),
                ServerDefaultPushRules.IsRoomMention.rule.copy(enabled = mention.room),
            )
        )
    }
}

expect fun platformNotificationSettingsSingleAccountViewModelFactoryModule(): Module
