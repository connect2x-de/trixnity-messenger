package de.connect2x.trixnity.messenger.internal.logic

import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.internal.navigation.InitialRoutes
import de.connect2x.trixnity.messenger.internal.navigation.Route
import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.routes.TimelineRoute
import de.connect2x.trixnity.messenger.internal.routes.extras.RoomSettingsRoute
import de.connect2x.trixnity.messenger.internal.routes.roomlist.RoomListRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest

class SelectedRoomIdLogicTest {

    private val dummyUserId = UserId("dummyUserId")
    private val dummyRoomId = RoomId("dummyRoomId")

    @Test
    fun `selectedRoomId should represent the selected roomId`() = runTest {
        val cut = cut()

        assertEquals(null, cut.selectedRoomIdLogic.selectedRoomId)

        cut.routeNavigation.updateNavigation { push(TimelineRoute(dummyUserId, dummyRoomId)) }
        assertEquals(dummyRoomId, cut.selectedRoomIdLogic.selectedRoomId)

        cut.routeNavigation.updateNavigation { push(RoomSettingsRoute(dummyUserId, dummyRoomId)) }
        assertEquals(dummyRoomId, cut.selectedRoomIdLogic.selectedRoomId)

        cut.routeNavigation.updateNavigation { items = listOf(RoomListRoute) }
        assertEquals(null, cut.selectedRoomIdLogic.selectedRoomId)
    }

    @Test
    fun `selectedRoomIdFlow should represent the selected roomId`() = runTest {
        val cut = cut()
        val roomIds = mutableListOf<RoomId?>()

        backgroundScope.launch { cut.selectedRoomIdLogic.selectedRoomIdFlow().collect { roomIds += it } }
        testScheduler.runCurrent()
        assertEquals(listOf<RoomId?>(null), roomIds)

        cut.routeNavigation.updateNavigation { push(TimelineRoute(dummyUserId, dummyRoomId)) }
        testScheduler.runCurrent()
        assertEquals(listOf(null, dummyRoomId), roomIds)

        cut.routeNavigation.updateNavigation { push(RoomSettingsRoute(dummyUserId, dummyRoomId)) }
        testScheduler.runCurrent()
        assertEquals(listOf(null, dummyRoomId), roomIds)

        cut.routeNavigation.updateNavigation { items = listOf(RoomListRoute) }
        testScheduler.runCurrent()
        assertEquals(listOf(null, dummyRoomId, null), roomIds)
    }

    private fun cut(initialRoutes: List<Route> = listOf(RoomListRoute)): Cut {
        val routeNavigation = RouteNavigation(initialRoutes = InitialRoutes(initialRoutes))
        val selectedRoomIdLogic = SelectedRoomIdLogic(routeNavigation = routeNavigation)

        return Cut(routeNavigation = routeNavigation, selectedRoomIdLogic = selectedRoomIdLogic)
    }

    private class Cut(val routeNavigation: RouteNavigation, val selectedRoomIdLogic: SelectedRoomIdLogic)
}
