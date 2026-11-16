package de.connect2x.trixnity.messenger.call

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.core.MSC4143
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.m.rtc.CallRtcApplication
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.runTestWithCoroutineScope
import dev.mokkery.mock
import kotlin.test.Test
import kotlin.test.assertEquals
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

    @Test
    fun `account removal and profile cancellation unregister the call`() = runTestWithCoroutineScope { profile ->
        val accounts = MutableStateFlow(mapOf(target.userId to mock<MatrixClient>()))
        val service = CallServiceImpl()
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
        val service = CallServiceImpl()
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
        val factory = CallMediaFactory { scope, _, request ->
            scope.launch { if (request(CallPermissionKind.MICROPHONE)) allowed++ }
            TestCallMedia()
        }
        fun call(): CallSession {
            return testCall(target, mediaFactory = factory)
        }
        val service = CallServiceImpl()
        val first = call()
        assertTrue(service.register(first))
        delay(2.seconds)
        val old = assertNotNull(first.permission.value)
        service.unregister(first)
        val next = call()
        assertTrue(service.register(next))
        delay(2.seconds)
        val request = assertNotNull(next.permission.value)
        old.second.complete(true)
        assertSame(request, next.permission.value)
        request.second.complete(true)
        delay(2.seconds)
        assertNull(next.permission.value)
        request.second.complete(false)
        delay(2.seconds)
        assertEquals(1, allowed)
        service.close()
    }
}
