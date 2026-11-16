package de.connect2x.trixnity.messenger.viewmodel.room.call

import de.connect2x.trixnity.core.MSC4143
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.m.rtc.CallRtcApplication
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.call.CallService
import de.connect2x.trixnity.messenger.call.CallServiceImpl
import de.connect2x.trixnity.messenger.call.CallTarget
import de.connect2x.trixnity.messenger.call.TestCallMedia
import de.connect2x.trixnity.messenger.call.testCall
import de.connect2x.trixnity.messenger.internal.adapters.RoomCallViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.navigation.InitialRoutes
import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.restoreCall
import de.connect2x.trixnity.messenger.internal.routes.RoomCallRoute
import de.connect2x.trixnity.messenger.internal.routes.TimelineRoute
import de.connect2x.trixnity.messenger.internal.routes.roomlist.RoomListRoute
import de.connect2x.trixnity.messenger.runTestWithCoroutineScope
import de.connect2x.trixnity.messenger.testViewModelContext
import de.connect2x.trixnity.messenger.util.BackHandler
import de.connect2x.trixnity.messenger.util.BackHandlerImpl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.koin.core.parameter.parametersOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@OptIn(TrixnityMessengerCallApi::class, MSC4143::class)
class RoomCallViewModelTest {
    @Test
    fun `expanded controls survive view disposal and cannot control a call in another room`() =
        runTestWithCoroutineScope { callScope ->
            val calls = CallServiceImpl(RouteNavigation(InitialRoutes(listOf(RoomListRoute))))
            val di =
                koinApplication {
                        modules(
                            module {
                                single<CallService> { calls }
                                single<BackHandler> { BackHandlerImpl() }
                            }
                        )
                    }
                    .koin
            val context = testViewModelContext(di)
            val target =
                CallTarget(UserId("alice", "example.org"), RoomId("!call:example.org"), CallRtcApplication.SLOT_ID)
            val media = TestCallMedia()
            val first = testCall(target, media = media, scope = callScope)
            assertTrue(calls.register(first))
            var backs = 0
            val viewModel =
                RoomCallViewModelImpl(
                    viewModelContext = context,
                    target = first.target,
                    calls = calls,
                    onBack = { backs++ },
                    onShowChat = {},
                )
            backgroundScope.launch { viewModel.state.collect {} }
            delay(2.seconds)
            assertSame(first.state.value, viewModel.state.value)
            viewModel.toggleMicrophone()
            viewModel.toggleCamera()
            viewModel.back()
            assertEquals(1, media.microphoneToggles)
            assertEquals(1, media.cameraToggles)
            assertEquals(1, backs)
            assertSame(first, calls.session.value)
            context.coroutineScope.cancel()
            delay(2.seconds)
            assertSame(first, calls.session.value)
            assertEquals(0, media.closes)
            calls.unregister(first)
            val nextMedia = TestCallMedia()
            val next =
                testCall(target.copy(roomId = RoomId("!other:example.org")), media = nextMedia, scope = callScope)
            assertTrue(calls.register(next))
            delay(2.seconds)
            viewModel.toggleMicrophone()
            viewModel.toggleCamera()
            viewModel.leave()
            assertSame(next, calls.session.value)
            assertEquals(0, nextMedia.microphoneToggles)
            assertEquals(0, nextMedia.cameraToggles)
            calls.unregister(next)
            assertNull(calls.session.value)
        }

    @Test
    fun `chat action opens the call room while Back preserves the previous chat`() = runTest {
        val target = CallTarget(UserId("alice", "example.org"), RoomId("!call:example.org"), CallRtcApplication.SLOT_ID)
        val otherChat = TimelineRoute(target.userId, RoomId("!other:example.org"))
        val route = RoomCallRoute(target)
        val navigation = RouteNavigation(InitialRoutes(listOf(RoomListRoute, otherChat, route)))
        val calls = CallServiceImpl(navigation)
        val backHandler = BackHandlerImpl()
        val di =
            koinApplication {
                    modules(
                        module {
                            single<CallService> { calls }
                            single<BackHandler> { backHandler }
                        }
                    )
                }
                .koin
        val call = testCall(target)
        assertTrue(calls.register(call))
        val viewModel =
            RoomCallViewModelFactoryAdapter(factory = RoomCallViewModelFactory, navigation = navigation)
                .create(parametersOf(route, testViewModelContext(di)))
        delay(2.seconds)

        viewModel.back()
        assertEquals(listOf(RoomListRoute, otherChat), navigation.routes.value)
        navigation.updateNavigation { restoreCall(target) }
        delay(2.seconds)
        backHandler.goBack()
        assertEquals(listOf(RoomListRoute, otherChat), navigation.routes.value)
        navigation.updateNavigation { restoreCall(target) }
        viewModel.showChat()
        assertEquals(listOf(RoomListRoute, TimelineRoute(target.userId, target.roomId)), navigation.routes.value)
        assertSame(call, calls.session.value)
    }

    @Test
    fun `ending the call closes its route and stale routes are discarded`() = runTest {
        val target = CallTarget(UserId("alice", "example.org"), RoomId("!call:example.org"), CallRtcApplication.SLOT_ID)
        val otherChat = TimelineRoute(target.userId, RoomId("!other:example.org"))
        val route = RoomCallRoute(target)
        val navigation = RouteNavigation(InitialRoutes(listOf(RoomListRoute, otherChat, route)))
        val calls = CallServiceImpl(navigation)
        val di =
            koinApplication {
                    modules(
                        module {
                            single<CallService> { calls }
                            single<BackHandler> { BackHandlerImpl() }
                        }
                    )
                }
                .koin
        val call = testCall(target)
        assertTrue(calls.register(call))
        val adapter = RoomCallViewModelFactoryAdapter(factory = RoomCallViewModelFactory, navigation = navigation)
        val viewModel = adapter.create(parametersOf(route, testViewModelContext(di)))
        delay(2.seconds)
        viewModel.leave()
        delay(2.seconds)
        assertNull(calls.session.value)
        assertEquals(listOf(RoomListRoute, otherChat), navigation.routes.value)

        navigation.updateNavigation { restoreCall(target) }
        adapter.create(parametersOf(route, testViewModelContext(di)))
        delay(2.seconds)
        assertEquals(listOf(RoomListRoute, otherChat), navigation.routes.value)
    }
}
