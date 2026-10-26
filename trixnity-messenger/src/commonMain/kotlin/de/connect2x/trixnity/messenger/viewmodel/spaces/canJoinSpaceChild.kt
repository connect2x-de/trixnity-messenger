package de.connect2x.trixnity.messenger.viewmodel.spaces

import de.connect2x.trixnity.core.model.events.m.room.CreateEventContent.RoomType
import de.connect2x.trixnity.core.model.events.m.room.JoinRulesEventContent.JoinRule
import de.connect2x.trixnity.core.model.events.m.room.Membership

internal fun canJoinSpaceChild(
    membership: Membership?,
    rule: JoinRule?,
    roomType: RoomType?,
    allowedByRoomMembership: Boolean,
): Boolean {
    val supported = roomType == null || roomType == RoomType.Room || roomType == RoomType.Space

    return supported &&
        membership !in setOf(Membership.JOIN, Membership.BAN) &&
        (membership == Membership.INVITE ||
            rule == JoinRule.Public ||
            (rule in setOf(JoinRule.Restricted, JoinRule.KnockRestricted) && allowedByRoomMembership))
}
