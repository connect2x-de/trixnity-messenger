package de.connect2x.trixnity.messenger.util

import net.folivo.trixnity.core.model.events.m.room.JoinRulesEventContent.JoinRule

val JoinRule.isKnock: Boolean
    get() =
        when (this) {
            JoinRule.Knock -> true
            JoinRule.KnockRestricted -> true
            else -> false
        }

