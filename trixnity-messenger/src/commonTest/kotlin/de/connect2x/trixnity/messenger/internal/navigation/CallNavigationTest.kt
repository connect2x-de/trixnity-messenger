package de.connect2x.trixnity.messenger.internal.navigation

import de.connect2x.trixnity.core.MSC4143
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.m.rtc.CallRtcApplication
import de.connect2x.trixnity.core.model.events.m.rtc.RtcSlotId
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.call.CallTarget
import de.connect2x.trixnity.messenger.internal.routes.RoomCallRoute
import de.connect2x.trixnity.messenger.internal.routes.TimelineRoute
import de.connect2x.trixnity.messenger.internal.routes.roomlist.RoomListRoute
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(TrixnityMessengerCallApi::class, MSC4143::class)
class CallNavigationTest {
    private val user = UserId("alice", "example.org")
    private val room = RoomId("!call:example.org")
    private val target = CallTarget(user, room, CallRtcApplication.SLOT_ID)
    private val call = RoomCallRoute(target)
    private val timeline = TimelineRoute(user, room)

    @Test
    fun `call navigation distinguishes slots in the same room`() {
        val breakout = target.copy(slotId = RtcSlotId("breakout"))
        val scope = RouteNavigationScope(listOf(RoomListRoute, timeline, RoomCallRoute(breakout)))
        scope.removeCall(target)
        assertEquals(listOf(RoomListRoute, timeline, RoomCallRoute(breakout)), scope.items)
        scope.restoreCall(breakout)
        assertEquals(listOf(RoomListRoute, timeline, RoomCallRoute(breakout)), scope.items)
        scope.removeCall(breakout)
        assertEquals(listOf(RoomListRoute, timeline), scope.items)
    }

    @Test
    fun `expanded call and chat are ordinary navigation destinations`() {
        val scope = RouteNavigationScope(listOf(RoomListRoute, timeline))
        scope.restoreCall(target)
        assertEquals(listOf(RoomListRoute, timeline, call), scope.items)
        scope.removeCall(target)
        assertEquals(listOf(RoomListRoute, timeline), scope.items)
        scope.closeRoom()
        assertEquals(listOf(RoomListRoute), scope.items)
        scope.restoreCall(target)
        assertEquals(listOf(RoomListRoute, call), scope.items)
        scope.removeCall(call.target)
        assertEquals(listOf(RoomListRoute), scope.items)
    }

    @Test
    fun `stale cleanup does not remove a call in another room`() {
        val next = call.copy(target = target.copy(roomId = RoomId("!other:example.org")))
        val scope = RouteNavigationScope(listOf(RoomListRoute, timeline, next))
        scope.removeCall(call.target)
        assertEquals(listOf(RoomListRoute, timeline, next), scope.items)
    }

    @Test
    fun `returning from a restored call preserves the chat navigated to while backgrounded`() {
        val other = TimelineRoute(user, RoomId("!other:example.org"))
        val scope = RouteNavigationScope(listOf(RoomListRoute, timeline))
        scope.restoreCall(target)
        scope.removeCall(target)
        scope.openRoom(other.userId, other.roomId)
        scope.restoreCall(target)
        scope.restoreCall(target)
        assertEquals(listOf(RoomListRoute, other, call), scope.items)
        scope.removeCall(target)
        assertEquals(listOf(RoomListRoute, other), scope.items)
    }

    @Test
    fun `removing a stale call only route leaves a valid root`() {
        val scope = RouteNavigationScope(listOf(call))
        scope.removeCall(call.target)
        assertEquals(listOf(RoomListRoute), scope.items)
    }
}
