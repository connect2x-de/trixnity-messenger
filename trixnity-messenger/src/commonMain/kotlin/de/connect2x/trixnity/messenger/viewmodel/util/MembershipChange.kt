package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.core.model.events.m.room.Membership

internal enum class MembershipChange {
    INVITE,
    JOIN,
    BAN,
    KNOCK,
    UNBAN,
    INVITE_REJECT,
    INVITE_REVOKE,
    LEAVE,
    KICK;

    companion object {
        // Possible state transitions:
        // * -> INVITE => Invite
        // * -> JOIN => Join
        // * -> BAN => Ban
        // * -> KNOCK => Knock
        // BAN -> LEAVE => Unban
        // INVITE -> LEAVE (self) => Invite Reject
        // INVITE -> LEAVE (other) => Invite Revoke
        // * -> LEAVE => Leave
        fun of(from: Membership?, to: Membership, appliedToSelf: Boolean): MembershipChange =
            when (to) {
                Membership.INVITE -> INVITE
                Membership.JOIN -> JOIN
                Membership.LEAVE ->
                    when (from) {
                        Membership.BAN -> UNBAN
                        Membership.INVITE -> if (appliedToSelf) INVITE_REJECT else INVITE_REVOKE
                        else -> if (appliedToSelf) LEAVE else KICK
                    }

                Membership.BAN -> BAN
                Membership.KNOCK -> KNOCK
            }
    }
}
