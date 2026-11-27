package de.connect2x.trixnity.messenger.call

import de.connect2x.trixnity.core.MSC4143
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.m.rtc.CallRtcApplication
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.internal.navigation.InitialRoutes
import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.restoreCall
import de.connect2x.trixnity.messenger.internal.routes.RoomCallRoute
import de.connect2x.trixnity.messenger.internal.routes.roomlist.RoomListRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest

@OptIn(TrixnityMessengerCallApi::class, MSC4143::class)
class CallServiceTest {
    private val target =
        CallTarget(UserId("alice", "example.org"), RoomId("!room:example.org"), CallRtcApplication.SLOT_ID)
    private val navigation = RouteNavigation(InitialRoutes(listOf(RoomListRoute)))

    @Test
    fun `registering keeps one call alive without subscribers`() = runTest {
        val service = CallServiceImpl(navigation)
        val rtc = TestCallSignalling()
        val media = TestCallMedia()
        val call = testCall(target, rtc, media)
        val rejectedRtc = TestCallSignalling()
        val rejected = testCall(target, rejectedRtc)
        assertTrue(service.register(call))
        assertFalse(service.register(rejected))
        rejected.close()
        media.state.value = CallMediaState(cameraEnabled = true)
        rtc.error.value = "signalling failed"
        delay(2.seconds)
        assertSame(call, service.session.value)
        assertTrue(call.state.value.media.cameraEnabled)
        assertEquals("signalling failed", call.state.value.error)
        assertEquals(0, rtc.closes)
        assertEquals(0, rejectedRtc.closes)
        service.close()
        delay(2.seconds)
        assertEquals(1, rtc.closes)
        assertEquals(1, media.closes)
        assertNull(service.session.value)
    }

    @Test
    fun `unregistering is idempotent and cannot remove a replacement`() = runTest {
        val service = CallServiceImpl(navigation)
        val signalling = TestCallSignalling()
        val first = testCall(target, signalling = signalling)
        assertTrue(service.register(first))
        navigation.updateNavigation { restoreCall(target) }
        delay(2.seconds)
        service.unregister(first)
        assertNull(service.session.value)
        assertEquals(listOf(RoomListRoute), navigation.routes.value)
        val next = testCall(target)
        assertTrue(service.register(next))
        navigation.updateNavigation { restoreCall(target) }
        service.unregister(first)
        delay(2.seconds) // The old call's completion callback also arrives after replacement.
        assertSame(next, service.session.value)
        assertEquals(listOf(RoomListRoute, RoomCallRoute(target)), navigation.routes.value)
        assertEquals(1, signalling.closes)
        service.close()
    }
}
