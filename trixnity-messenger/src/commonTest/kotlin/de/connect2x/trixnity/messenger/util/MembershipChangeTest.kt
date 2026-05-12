package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.viewmodel.util.MembershipChange
import de.connect2x.trixnity.core.model.events.m.room.Membership
import kotlin.test.Test
import kotlin.test.assertEquals

// Possible state transitions:
// * -> INVITE => Invite
// * -> JOIN => Join
// * -> BAN => Ban
// * -> KNOCK => Knock
// BAN -> LEAVE => Unban
// INVITE -> LEAVE (self) => Invite Reject
// INVITE -> LEAVE (other) => Invite Revoke
// * -> LEAVE => Leave
class MembershipChangeTest {
    @Test
    fun testInvite() {
        assertEquals(MembershipChange.INVITE, MembershipChange.of(null, Membership.INVITE, false))
        assertEquals(MembershipChange.INVITE, MembershipChange.of(Membership.JOIN, Membership.INVITE, false))
        assertEquals(MembershipChange.INVITE, MembershipChange.of(Membership.INVITE, Membership.INVITE, false))
        assertEquals(MembershipChange.INVITE, MembershipChange.of(Membership.KNOCK, Membership.INVITE, false))
        assertEquals(MembershipChange.INVITE, MembershipChange.of(Membership.LEAVE, Membership.INVITE, false))
        assertEquals(MembershipChange.INVITE, MembershipChange.of(Membership.BAN, Membership.INVITE, false))

        assertEquals(MembershipChange.INVITE, MembershipChange.of(null, Membership.INVITE, true))
        assertEquals(MembershipChange.INVITE, MembershipChange.of(Membership.JOIN, Membership.INVITE, true))
        assertEquals(MembershipChange.INVITE, MembershipChange.of(Membership.INVITE, Membership.INVITE, true))
        assertEquals(MembershipChange.INVITE, MembershipChange.of(Membership.KNOCK, Membership.INVITE, true))
        assertEquals(MembershipChange.INVITE, MembershipChange.of(Membership.LEAVE, Membership.INVITE, true))
        assertEquals(MembershipChange.INVITE, MembershipChange.of(Membership.BAN, Membership.INVITE, true))
    }

    @Test
    fun testJoin() {
        assertEquals(MembershipChange.JOIN, MembershipChange.of(null, Membership.JOIN, false))
        assertEquals(MembershipChange.JOIN, MembershipChange.of(Membership.INVITE, Membership.JOIN, false))
        assertEquals(MembershipChange.JOIN, MembershipChange.of(Membership.JOIN, Membership.JOIN, false))
        assertEquals(MembershipChange.JOIN, MembershipChange.of(Membership.KNOCK, Membership.JOIN, false))
        assertEquals(MembershipChange.JOIN, MembershipChange.of(Membership.LEAVE, Membership.JOIN, false))
        assertEquals(MembershipChange.JOIN, MembershipChange.of(Membership.BAN, Membership.JOIN, false))

        assertEquals(MembershipChange.JOIN, MembershipChange.of(null, Membership.JOIN, true))
        assertEquals(MembershipChange.JOIN, MembershipChange.of(Membership.INVITE, Membership.JOIN, true))
        assertEquals(MembershipChange.JOIN, MembershipChange.of(Membership.JOIN, Membership.JOIN, true))
        assertEquals(MembershipChange.JOIN, MembershipChange.of(Membership.KNOCK, Membership.JOIN, true))
        assertEquals(MembershipChange.JOIN, MembershipChange.of(Membership.LEAVE, Membership.JOIN, true))
        assertEquals(MembershipChange.JOIN, MembershipChange.of(Membership.BAN, Membership.JOIN, true))
    }

    @Test
    fun testKnock() {
        assertEquals(MembershipChange.KNOCK, MembershipChange.of(null, Membership.KNOCK, false))
        assertEquals(MembershipChange.KNOCK, MembershipChange.of(Membership.INVITE, Membership.KNOCK, false))
        assertEquals(MembershipChange.KNOCK, MembershipChange.of(Membership.JOIN, Membership.KNOCK, false))
        assertEquals(MembershipChange.KNOCK, MembershipChange.of(Membership.KNOCK, Membership.KNOCK, false))
        assertEquals(MembershipChange.KNOCK, MembershipChange.of(Membership.LEAVE, Membership.KNOCK, false))
        assertEquals(MembershipChange.KNOCK, MembershipChange.of(Membership.BAN, Membership.KNOCK, false))

        assertEquals(MembershipChange.KNOCK, MembershipChange.of(null, Membership.KNOCK, true))
        assertEquals(MembershipChange.KNOCK, MembershipChange.of(Membership.INVITE, Membership.KNOCK, true))
        assertEquals(MembershipChange.KNOCK, MembershipChange.of(Membership.JOIN, Membership.KNOCK, true))
        assertEquals(MembershipChange.KNOCK, MembershipChange.of(Membership.KNOCK, Membership.KNOCK, true))
        assertEquals(MembershipChange.KNOCK, MembershipChange.of(Membership.LEAVE, Membership.KNOCK, true))
        assertEquals(MembershipChange.KNOCK, MembershipChange.of(Membership.BAN, Membership.KNOCK, true))
    }

    @Test
    fun testLeave() {
        assertEquals(MembershipChange.KICK, MembershipChange.of(null, Membership.LEAVE, false))
        assertEquals(MembershipChange.INVITE_REVOKE, MembershipChange.of(Membership.INVITE, Membership.LEAVE, false))
        assertEquals(MembershipChange.KICK, MembershipChange.of(Membership.JOIN, Membership.LEAVE, false))
        assertEquals(MembershipChange.KICK, MembershipChange.of(Membership.KNOCK, Membership.LEAVE, false))
        assertEquals(MembershipChange.KICK, MembershipChange.of(Membership.LEAVE, Membership.LEAVE, false))
        assertEquals(MembershipChange.UNBAN, MembershipChange.of(Membership.BAN, Membership.LEAVE, false))

        assertEquals(MembershipChange.LEAVE, MembershipChange.of(null, Membership.LEAVE, true))
        assertEquals(MembershipChange.INVITE_REJECT, MembershipChange.of(Membership.INVITE, Membership.LEAVE, true))
        assertEquals(MembershipChange.LEAVE, MembershipChange.of(Membership.JOIN, Membership.LEAVE, true))
        assertEquals(MembershipChange.LEAVE, MembershipChange.of(Membership.KNOCK, Membership.LEAVE, true))
        assertEquals(MembershipChange.LEAVE, MembershipChange.of(Membership.LEAVE, Membership.LEAVE, true))
        assertEquals(MembershipChange.UNBAN, MembershipChange.of(Membership.BAN, Membership.LEAVE, true))
    }

    @Test
    fun testBan() {
        assertEquals(MembershipChange.BAN, MembershipChange.of(null, Membership.BAN, false))
        assertEquals(MembershipChange.BAN, MembershipChange.of(Membership.INVITE, Membership.BAN, false))
        assertEquals(MembershipChange.BAN, MembershipChange.of(Membership.JOIN, Membership.BAN, false))
        assertEquals(MembershipChange.BAN, MembershipChange.of(Membership.KNOCK, Membership.BAN, false))
        assertEquals(MembershipChange.BAN, MembershipChange.of(Membership.LEAVE, Membership.BAN, false))
        assertEquals(MembershipChange.BAN, MembershipChange.of(Membership.BAN, Membership.BAN, false))

        assertEquals(MembershipChange.BAN, MembershipChange.of(null, Membership.BAN, true))
        assertEquals(MembershipChange.BAN, MembershipChange.of(Membership.INVITE, Membership.BAN, true))
        assertEquals(MembershipChange.BAN, MembershipChange.of(Membership.JOIN, Membership.BAN, true))
        assertEquals(MembershipChange.BAN, MembershipChange.of(Membership.KNOCK, Membership.BAN, true))
        assertEquals(MembershipChange.BAN, MembershipChange.of(Membership.LEAVE, Membership.BAN, true))
        assertEquals(MembershipChange.BAN, MembershipChange.of(Membership.BAN, Membership.BAN, true))
    }
}
