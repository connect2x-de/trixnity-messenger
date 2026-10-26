package de.connect2x.trixnity.messenger.internal.worker

import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.internal.logic.AccountSyncStatesLogic
import de.connect2x.trixnity.messenger.internal.navigation.InitialRoutes
import de.connect2x.trixnity.messenger.internal.navigation.Route
import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.routes.SyncRoute
import de.connect2x.trixnity.messenger.internal.routes.TimelineRoute
import de.connect2x.trixnity.messenger.internal.routes.extras.RoomSettingsRoute
import de.connect2x.trixnity.messenger.internal.routes.roomlist.AppInfoRoute
import de.connect2x.trixnity.messenger.internal.routes.roomlist.RoomListRoute
import de.connect2x.trixnity.messenger.internal.routes.spaces.SpaceBrowserRoute
import de.connect2x.trixnity.messenger.internal.routes.spaces.SpaceExploreRoute
import de.connect2x.trixnity.messenger.internal.routes.spaces.SpaceRoomListRoute
import de.connect2x.trixnity.messenger.internal.util.MinimizeAppIfPossible
import de.connect2x.trixnity.messenger.internal.workers.FallbackBackCallbackWorker
import de.connect2x.trixnity.messenger.internal.workers.ShowSyncWorker
import de.connect2x.trixnity.messenger.util.BackCallback
import de.connect2x.trixnity.messenger.util.BackHandlerImpl
import de.connect2x.trixnity.messenger.viewmodel.initialsync.AccountSync
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import dev.mokkery.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class FallbackBackCallbackWorkerTest {
    @Test
    fun `fallback outside spaces retains the existing return to room list behavior`() = runTest {
        val user = UserId("alice", "example.org")
        val room = RoomId("!room:example.org")
        val navigation =
            RouteNavigation(
                InitialRoutes(listOf(RoomListRoute, TimelineRoute(user, room), RoomSettingsRoute(user, room)))
            )
        val handler = BackHandlerImpl()
        val worker = FallbackBackCallbackWorker(navigation, handler, mock<MinimizeAppIfPossible>())
        backgroundScope.launch { worker.doWork() }
        runCurrent()
        handler.goBack()
        runCurrent()
        assertEquals(listOf(RoomListRoute), navigation.routes.value)
    }

    @Test
    fun `back from a timeline preserves the selected space`() = runTest {
        val user = UserId("alice", "example.org")
        val space = SpaceRoomListRoute(user, RoomId("!space:example.org"))
        val previous = listOf(RoomListRoute, space)
        val navigation = RouteNavigation(InitialRoutes(previous + TimelineRoute(user, RoomId("!room:example.org"))))
        val handler = BackHandlerImpl()
        val worker = FallbackBackCallbackWorker(navigation, handler, mock<MinimizeAppIfPossible>())
        backgroundScope.launch { worker.doWork() }
        runCurrent()
        handler.goBack()
        runCurrent()
        assertEquals(previous, navigation.routes.value)
    }

    @Test
    fun `fallback walks nested spaces back to home then minimizes`() = runTest {
        val user = UserId("alice", "example.org")
        val space = SpaceRoomListRoute(user, RoomId("!space:example.org"))
        val routes = listOf(RoomListRoute, SpaceBrowserRoute, space, SpaceExploreRoute(user, space.spaceId))
        val navigation = RouteNavigation(InitialRoutes(routes))
        val handler = BackHandlerImpl()
        val minimize = mock<MinimizeAppIfPossible>()
        every { minimize.invoke() } returns Unit
        val worker = FallbackBackCallbackWorker(navigation, handler, minimize)
        backgroundScope.launch { worker.doWork() }
        runCurrent()
        for (size in routes.size - 1 downTo 1) {
            handler.goBack()
            runCurrent()
            assertEquals(routes.take(size), navigation.routes.value)
        }
        handler.goBack()
        runCurrent()
        assertEquals(listOf(RoomListRoute), navigation.routes.value)
        verify { minimize.invoke() }
    }

    @Test
    fun `back during initial sync leaves the route in place`() = runTest {
        val user = UserId("alice", "example.org")
        val initial: List<Route> = listOf(RoomListRoute, SyncRoute)
        val navigation = RouteNavigation(InitialRoutes(initial))
        val observedRoutes = mutableListOf<List<Route>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            navigation.routes.collect { observedRoutes += it }
        }
        val handler = BackHandlerImpl()
        backgroundScope.launch { FallbackBackCallbackWorker(navigation, handler, mock()).doWork() }
        val states = MutableStateFlow(mapOf(user to AccountSync.INITIAL_SYNC))
        val syncStates =
            object : AccountSyncStatesLogic {
                override fun accountSyncStates() = states
            }
        backgroundScope.launch { ShowSyncWorker(navigation, syncStates).doWork() }
        runCurrent()
        handler.goBack()
        runCurrent()
        assertEquals(listOf(initial), observedRoutes)

        states.value = mapOf(user to AccountSync.DONE)
        runCurrent()
        assertEquals(listOf(initial, listOf(RoomListRoute)), observedRoutes)
    }

    @Test
    fun `fallback leaves other screens without a timeline unchanged`() = runTest {
        val initial = listOf(RoomListRoute, AppInfoRoute)
        val navigation = RouteNavigation(InitialRoutes(initial))
        val handler = BackHandlerImpl()
        backgroundScope.launch { FallbackBackCallbackWorker(navigation, handler, mock()).doWork() }
        runCurrent()
        handler.goBack()
        runCurrent()
        assertEquals(initial, navigation.routes.value)
    }

    @Test
    fun `screen callbacks take precedence over the fallback`() = runTest {
        val routes = listOf(RoomListRoute, SpaceBrowserRoute)
        val navigation = RouteNavigation(InitialRoutes(routes))
        val handler = BackHandlerImpl()
        val worker = FallbackBackCallbackWorker(navigation, handler, mock())
        backgroundScope.launch { worker.doWork() }
        runCurrent()
        var handled = false
        handler.registerBackCallback(BackCallback { handled = true })
        handler.goBack()
        runCurrent()
        assertEquals(true, handled)
        assertEquals(routes, navigation.routes.value)
    }
}
