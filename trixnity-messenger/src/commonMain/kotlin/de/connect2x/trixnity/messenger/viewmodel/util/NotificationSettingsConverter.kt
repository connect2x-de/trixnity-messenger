package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.push.PushAction
import de.connect2x.trixnity.core.model.push.PushRule
import de.connect2x.trixnity.core.model.push.PushRuleSet
import de.connect2x.trixnity.core.model.push.ServerDefaultPushRules
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountNotificationSettings
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountNotificationSettings.DefaultLevel

internal fun PushRuleSet.toNotificationSettings(): AccountNotificationSettings {
    val serverDefaultRules = getServerDefaultRules()
    val dmRules =
        setOfNotNull(
            serverDefaultRules[ServerDefaultPushRules.RoomOneToOne.id],
            serverDefaultRules[ServerDefaultPushRules.EncryptedRoomOneToOne.id],
        )
    val roomRules =
        setOfNotNull(
            serverDefaultRules[ServerDefaultPushRules.Message.id],
            serverDefaultRules[ServerDefaultPushRules.Encrypted.id],
        )
    val contentRules = getContentRules().values

    return AccountNotificationSettings(
        defaultLevel =
            when {
                serverDefaultRules[ServerDefaultPushRules.Master.id]?.enabled == true -> DefaultLevel.NONE
                roomRules.shouldNotify() -> DefaultLevel.ROOM
                dmRules.shouldNotify() -> DefaultLevel.DM
                else -> DefaultLevel.MENTION
            },
        sound =
            AccountNotificationSettings.Sound(
                room = roomRules.shouldSetSoundTweak(),
                dm = dmRules.shouldSetSoundTweak(),
                mention =
                    (setOfNotNull(
                            serverDefaultRules[ServerDefaultPushRules.IsUserMention.id],
                            serverDefaultRules[ServerDefaultPushRules.IsRoomMention.id],
                        ) + contentRules)
                        .shouldSetSoundTweak(),
                call = setOfNotNull(serverDefaultRules[ServerDefaultPushRules.Call.id]).shouldSetSoundTweak(),
            ),
        activity =
            AccountNotificationSettings.Activity(
                invite = setOfNotNull(serverDefaultRules[ServerDefaultPushRules.InviteForMe.id]).shouldNotify(),
                status =
                    setOfNotNull(
                            serverDefaultRules[ServerDefaultPushRules.MemberEvent.id],
                            serverDefaultRules[ServerDefaultPushRules.Tombstone.id],
                        )
                        .shouldNotify(),
                notice = serverDefaultRules[ServerDefaultPushRules.SuppressNotice.id]?.enabled == false,
            ),
        mention =
            AccountNotificationSettings.Mention(
                user = setOfNotNull(serverDefaultRules[ServerDefaultPushRules.IsUserMention.id]).shouldNotify(),
                room = setOfNotNull(serverDefaultRules[ServerDefaultPushRules.IsRoomMention.id]).shouldNotify(),
                keyword = contentRules.shouldNotify(),
            ),
        keywords = contentRules.map { it.pattern }.toSet(),
    )
}

internal fun PushRuleSet.getServerDefaultRules() =
    (override.orEmpty() + underride.orEmpty()).filter { it.ruleId.startsWith(".") }.associateBy { it.ruleId }

internal fun PushRuleSet.getContentRules() =
    content.orEmpty().filterNot { it.ruleId.startsWith(".") }.associateBy { it.ruleId }

private fun Collection<PushRule>.shouldNotify(): Boolean = any { it.enabled && it.actions.contains(PushAction.Notify) }

private fun Collection<PushRule>.shouldSetSoundTweak(): Boolean =
    filter { it.actions.contains(PushAction.Notify) }
        .firstNotNullOfOrNull { it.actions.filterIsInstance<PushAction.SetSoundTweak>().firstOrNull() }
        ?.value != null

internal fun AccountNotificationSettings.toPushRuleSet(userId: UserId): PushRuleSet =
    PushRuleSet(
        override =
            listOf(
                ServerDefaultPushRules.Master.rule.copy(enabled = defaultLevel == DefaultLevel.NONE),
                ServerDefaultPushRules.SuppressNotice.rule.copy(enabled = !activity.notice),
                ServerDefaultPushRules.InviteForMe(userId)
                    .rule
                    .copy(enabled = activity.invite, actions = actions(notify = true)),
                ServerDefaultPushRules.MemberEvent.rule.copy(
                    enabled = activity.status,
                    actions = actions(notify = true),
                ),
                ServerDefaultPushRules.Tombstone.rule.copy(enabled = activity.status, actions = actions(notify = true)),
                ServerDefaultPushRules.IsUserMention(userId)
                    .rule
                    .copy(
                        enabled = mention.user,
                        actions = actions(notify = true, sound = sound.mention, highlight = true),
                    ),
                ServerDefaultPushRules.IsRoomMention.rule.copy(
                    enabled = mention.room,
                    actions = actions(notify = true, sound = sound.mention),
                ),
            ),
        underride =
            listOf(
                ServerDefaultPushRules.Encrypted.rule.copy(
                    enabled = defaultLevel == DefaultLevel.ROOM,
                    actions = actions(notify = true, sound = sound.room),
                ),
                ServerDefaultPushRules.Message.rule.copy(
                    enabled = defaultLevel == DefaultLevel.ROOM,
                    actions = actions(notify = true, sound = sound.room),
                ),
                ServerDefaultPushRules.EncryptedRoomOneToOne.rule.copy(
                    enabled = defaultLevel == DefaultLevel.ROOM || defaultLevel == DefaultLevel.DM,
                    actions = actions(notify = true, sound = sound.dm),
                ),
                ServerDefaultPushRules.RoomOneToOne.rule.copy(
                    enabled = defaultLevel == DefaultLevel.ROOM || defaultLevel == DefaultLevel.DM,
                    actions = actions(notify = true, sound = sound.dm),
                ),
                ServerDefaultPushRules.Call.rule.copy(
                    enabled = sound.call,
                    actions = actions(notify = true, sound = sound.call, soundType = "ring"),
                ),
            ),
        content =
            keywords.map { keyword ->
                PushRule.Content(
                    ruleId = keyword,
                    default = false,
                    enabled = mention.keyword,
                    pattern = keyword,
                    actions = actions(notify = true, sound = sound.mention, highlight = true),
                )
            },
    )

private fun actions(
    notify: Boolean = false,
    sound: Boolean = false,
    soundType: String = "default",
    highlight: Boolean = false,
): Set<PushAction> =
    setOfNotNull(
        if (notify) PushAction.Notify else null,
        if (sound) PushAction.SetSoundTweak(soundType) else null,
        if (highlight) PushAction.SetHighlightTweak() else null,
    )
