package de.connect2x.trixnity.messenger.viewmodel.room.call

import de.connect2x.trixnity.core.MSC4143
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.m.rtc.CallRtcApplication
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.call.CallMediaState
import de.connect2x.trixnity.messenger.call.CallServiceImpl
import de.connect2x.trixnity.messenger.call.CallSession
import de.connect2x.trixnity.messenger.call.CallTarget
import de.connect2x.trixnity.messenger.call.TestCallMedia
import de.connect2x.trixnity.messenger.call.testCall
import de.connect2x.trixnity.messenger.internal.navigation.InitialRoutes
import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.routes.RoomCallRoute
import de.connect2x.trixnity.messenger.internal.routes.roomlist.RoomListRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest

@OptIn(TrixnityMessengerCallApi::class, MSC4143::class)
class CallControlsViewModelTest {
    private val target =
        CallTarget(UserId("alice", "example.org"), RoomId("!call:example.org"), CallRtcApplication.SLOT_ID)

    @Test
    fun factoryControlsAreDisposedWithTheirHostWhileTheCallKeepsRunning() = runTest {
        val navigation = RouteNavigation(InitialRoutes(listOf(RoomListRoute)))
        val calls = CallServiceImpl(navigation)
        val factory = CallControlsViewModelFactoryImpl(calls, navigation)
        fun controls(): Pair<CoroutineScope, CallControlsViewModel> {
            val scope = CoroutineScope(backgroundScope.coroutineContext + Job(backgroundScope.coroutineContext[Job]))
            val controls = factory.create(scope)
            scope.launch { controls.state.collect {} }
            return scope to controls
        }
        val media = TestCallMedia()
        val session = testCall(target, media = media)
        assertTrue(calls.register(session))
        val (owner, first) = controls()
        delay(2.seconds)
        first.openFullscreenCall()
        assertEquals(listOf(RoomListRoute, RoomCallRoute(target)), navigation.routes.value)
        val lastState = first.state.value
        owner.cancel()
        media.state.value = CallMediaState(microphoneEnabled = true)
        delay(2.seconds)
        assertSame(lastState, first.state.value)
        assertSame(session, calls.session.value)
        assertEquals(0, media.closes)
        val (newOwner, next) = controls()
        delay(2.seconds)
        assertTrue(next.state.value!!.media.microphoneEnabled)
        newOwner.cancel()
        calls.unregister(session)
    }

    @Test
    fun controlsFollowTheCurrentSessionAcrossRoomsAndBecomeEmptyAfterLeaving() = runTest {
        val navigation = RouteNavigation(InitialRoutes(listOf(RoomListRoute)))
        val calls = CallServiceImpl(navigation)
        val opened = mutableListOf<CallSession>()
        val controls = CallControlsViewModelImpl(backgroundScope, calls, opened::add)
        backgroundScope.launch { controls.state.collect {} }
        delay(2.seconds)
        assertNull(controls.state.value)
        val first = testCall(target)
        assertTrue(calls.register(first))
        delay(2.seconds)
        assertSame(first.state.value, controls.state.value)
        calls.unregister(first)
        delay(2.seconds)
        assertNull(controls.state.value)
        val media = TestCallMedia()
        val next = testCall(target.copy(roomId = RoomId("!other:example.org")), media = media)
        assertTrue(calls.register(next))
        delay(2.seconds)
        assertSame(next.state.value, controls.state.value)
        media.state.value = CallMediaState(microphoneEnabled = true)
        delay(2.seconds)
        assertTrue(controls.state.value!!.media.microphoneEnabled)
        controls.toggleMicrophone()
        assertEquals(1, media.microphoneToggles)
        controls.openFullscreenCall()
        assertEquals(listOf(next), opened)
        controls.leave()
        delay(2.seconds)
        assertNull(calls.session.value)
        assertNull(controls.state.value)
        assertEquals(1, media.closes)
    }

    @Test
    fun actionsWithoutACallDoNothing() = runTest {
        val navigation = RouteNavigation(InitialRoutes(listOf(RoomListRoute)))
        val calls = CallServiceImpl(navigation)
        val opened = mutableListOf<CallSession>()
        val controls = CallControlsViewModelImpl(backgroundScope, calls, opened::add)
        controls.toggleMicrophone()
        controls.openFullscreenCall()
        controls.leave()
        assertNull(calls.session.value)
        assertEquals(emptyList(), opened)
    }
}
