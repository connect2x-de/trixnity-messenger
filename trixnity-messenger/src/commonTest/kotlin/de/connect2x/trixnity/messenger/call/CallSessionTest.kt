package de.connect2x.trixnity.messenger.call

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.core.MSC4143
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.m.rtc.CallRtcApplication
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.internal.navigation.InitialRoutes
import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.routes.roomlist.RoomListRoute
import de.connect2x.trixnity.messenger.runTestWithCoroutineScope
import dev.mokkery.mock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest

@OptIn(TrixnityMessengerCallApi::class, MSC4143::class)
class CallSessionTest {
    private val target =
        CallTarget(UserId("alice", "example.org"), RoomId("!room:example.org"), CallRtcApplication.SLOT_ID)
    private val navigation = RouteNavigation(InitialRoutes(listOf(RoomListRoute)))

    @Test
    fun `account removal and profile cancellation unregister the call`() = runTestWithCoroutineScope { profile ->
        val accounts = MutableStateFlow(mapOf(target.userId to mock<MatrixClient>()))
        val service = CallServiceImpl(navigation)
        val rtc = TestCallSignalling()
        val media = TestCallMedia()
        val first = testCall(target, rtc, media, testCallClients(accounts), scope = profile)
        assertTrue(service.register(first))
        delay(2.seconds)
        accounts.value = emptyMap()
        delay(2.seconds)
        assertNull(service.session.value)
        assertEquals(1, rtc.closes)
        assertEquals(1, media.closes)
        accounts.value = mapOf(target.userId to mock<MatrixClient>())
        assertTrue(service.register(testCall(target, scope = profile)))
        delay(2.seconds)
        profile.cancel()
        delay(2.seconds)
        assertNull(service.session.value)
    }

    @Test
    fun `media construction failure releases signalling and remains dismissible`() = runTest {
        val rtc = TestCallSignalling()
        val call =
            testCall(target, signalling = rtc, mediaFactory = CallMediaFactory { _, _, _ -> error("media failed") })
        val service = CallServiceImpl(navigation)
        assertTrue(service.register(call))
        delay(2.seconds)
        assertEquals("Could not connect to the call.", call.state.value.error)
        assertEquals(1, rtc.closes)
        assertSame(call, service.session.value)
        service.unregister(call)
        delay(2.seconds)
        assertEquals(1, rtc.closes)
    }

    @Test
    fun `permission results belong to their registered call`() = runTest {
        var allowed = 0
        val factory = CallMediaFactory { scope, _, requestPermission ->
            scope.launch { requestPermission(CallPermissionKind.MICROPHONE) }
            TestCallMedia { _, granted -> if (granted) allowed++ }
        }
        fun call(): CallSession {
            return testCall(target, mediaFactory = factory)
        }
        val service = CallServiceImpl(navigation)
        val first = call()
        assertTrue(service.register(first))
        delay(2.seconds)
        val old = assertNotNull(first.permissionRequest.value)
        service.unregister(first)
        val next = call()
        assertTrue(service.register(next))
        delay(2.seconds)
        val request = assertNotNull(next.permissionRequest.value)
        first.onPermissionRequestResult(old, true)
        assertSame(request, next.permissionRequest.value)
        next.onPermissionRequestResult(CallPermissionKind.CAMERA, true)
        assertEquals(request, next.permissionRequest.value)
        next.onPermissionRequestResult(request, true)
        delay(2.seconds)
        assertNull(next.permissionRequest.value)
        next.onPermissionRequestResult(request, false)
        delay(2.seconds)
        assertEquals(1, allowed)
        service.close()
    }

    @Test
    fun `overlapping permission requests fail without replacing the pending request`() = runTest {
        val results = mutableListOf<Boolean>()
        val factory = CallMediaFactory { scope, _, requestPermission ->
            scope.launch { requestPermission(CallPermissionKind.MICROPHONE) }
            scope.launch { assertFailsWith<IllegalStateException> { requestPermission(CallPermissionKind.CAMERA) } }
            TestCallMedia { kind, granted ->
                results += granted
                if (kind == CallPermissionKind.MICROPHONE) requestPermission(CallPermissionKind.CAMERA)
            }
        }
        val service = CallServiceImpl(navigation)
        val call = testCall(target, mediaFactory = factory)
        assertTrue(service.register(call))
        delay(2.seconds)
        assertEquals(CallPermissionKind.MICROPHONE, call.permissionRequest.value)
        call.onPermissionRequestResult(CallPermissionKind.MICROPHONE, true)
        delay(2.seconds)
        assertEquals(CallPermissionKind.CAMERA, call.permissionRequest.value)
        call.onPermissionRequestResult(CallPermissionKind.MICROPHONE, false)
        assertEquals(CallPermissionKind.CAMERA, call.permissionRequest.value)
        call.onPermissionRequestResult(CallPermissionKind.CAMERA, false)
        delay(2.seconds)
        assertNull(call.permissionRequest.value)
        assertEquals(listOf(true, false), results)
        service.close()
    }
}
