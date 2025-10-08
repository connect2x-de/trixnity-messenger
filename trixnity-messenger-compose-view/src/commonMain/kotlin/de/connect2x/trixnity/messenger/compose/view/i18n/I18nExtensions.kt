package de.connect2x.trixnity.messenger.compose.view.i18n

import net.folivo.trixnity.core.model.events.m.room.HistoryVisibilityEventContent
import net.folivo.trixnity.core.model.events.m.room.JoinRulesEventContent

fun HistoryVisibilityEventContent.HistoryVisibility.getExplanation(i18n: I18nView): String {
    return when(this) {
        HistoryVisibilityEventContent.HistoryVisibility.INVITED -> i18n.historyVisibilityInvitedExplanation()
        HistoryVisibilityEventContent.HistoryVisibility.SHARED -> i18n.historyVisibilitySharedExplanation()
        HistoryVisibilityEventContent.HistoryVisibility.JOINED -> i18n.historyVisibilityJoinedExplanation()
        HistoryVisibilityEventContent.HistoryVisibility.WORLD_READABLE -> i18n.historyVisibilityWorldReadableExplanation()
    }
}

fun HistoryVisibilityEventContent.HistoryVisibility.getExplanationWhenEncrypted(i18n: I18nView) : String {
    return when(this) {
        HistoryVisibilityEventContent.HistoryVisibility.WORLD_READABLE -> i18n.historyVisibilityWorldReadableEncryptedExplanation()
        else -> this.getExplanation(i18n)
    }
}

fun HistoryVisibilityEventContent.HistoryVisibility.getStateName(i18n: I18nView): String {
    return when(this) {
        HistoryVisibilityEventContent.HistoryVisibility.INVITED -> i18n.historyVisibilityInvited()
        HistoryVisibilityEventContent.HistoryVisibility.SHARED -> i18n.historyVisibilityShared()
        HistoryVisibilityEventContent.HistoryVisibility.JOINED -> i18n.historyVisibilityJoined()
        HistoryVisibilityEventContent.HistoryVisibility.WORLD_READABLE -> i18n.historyVisibilityWorldReadable()
    }
}

fun JoinRulesEventContent.JoinRule.getExplanation(i18n: I18nView) : String {
    return when (this) {
        JoinRulesEventContent.JoinRule.Public -> i18n.joinRulePublicExplanation()
        JoinRulesEventContent.JoinRule.Invite -> i18n.joinRuleInvitedExplanation()
        JoinRulesEventContent.JoinRule.Knock -> i18n.joinRuleKnockExplanation()
        JoinRulesEventContent.JoinRule.KnockRestricted -> i18n.joinRuleKnockRestrictedExplanation()
        JoinRulesEventContent.JoinRule.Private -> i18n.joinRulePrivateExplanation()
        JoinRulesEventContent.JoinRule.Restricted -> i18n.joinRuleRestrictedExplanation()
        is JoinRulesEventContent.JoinRule.Unknown -> ""
    }
}
fun JoinRulesEventContent.JoinRule.getStateName(i18n: I18nView) : String {
    return when(this) {
        JoinRulesEventContent.JoinRule.Invite -> i18n.joinRuleInvited()
        JoinRulesEventContent.JoinRule.Knock -> i18n.joinRuleKnock()
        JoinRulesEventContent.JoinRule.KnockRestricted -> i18n.joinRuleKnockRestricted()
        JoinRulesEventContent.JoinRule.Private -> i18n.joinRulePrivate()
        JoinRulesEventContent.JoinRule.Public -> i18n.joinRulePublic()
        JoinRulesEventContent.JoinRule.Restricted -> i18n.joinRuleRestricted()
        is JoinRulesEventContent.JoinRule.Unknown -> ""
    }
}

