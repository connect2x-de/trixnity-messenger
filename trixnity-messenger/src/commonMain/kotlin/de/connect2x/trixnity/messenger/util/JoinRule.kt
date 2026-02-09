package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.core.model.events.m.room.JoinRulesEventContent.JoinRule

val JoinRule.isKnock: Boolean
    get() =
        when (this) {
            JoinRule.Knock, JoinRule.KnockRestricted -> true
            JoinRule.Invite, JoinRule.Private, JoinRule.Public, JoinRule.Restricted, is JoinRule.Unknown -> false
        }
