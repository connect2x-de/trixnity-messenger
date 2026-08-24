package de.connect2x.trixnity.messenger.internal.logic

import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.messenger.internal.navigation.Route
import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.routes.TimelineRoute
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal interface SelectedRoomIdLogic {
    val selectedRoomId: RoomId?

    fun selectedRoomIdFlow(): Flow<RoomId?>
}

internal fun SelectedRoomIdLogic(routeNavigation: RouteNavigation): SelectedRoomIdLogic {
    return SelectedRoomIdLogicImpl(routeNavigation = routeNavigation)
}

private class SelectedRoomIdLogicImpl(private val routeNavigation: RouteNavigation) : SelectedRoomIdLogic {
    override val selectedRoomId: RoomId?
        get() = timelineRoomIdOrNull(routeNavigation.routes.value)

    override fun selectedRoomIdFlow(): Flow<RoomId?> {
        return timelineRoomIdOrNull(routeNavigation.routes).distinctUntilChanged()
    }
}

private fun timelineRoomIdOrNull(routes: Flow<List<Route>>): Flow<RoomId?> {
    return routes.map(::timelineRoomIdOrNull)
}

private fun timelineRoomIdOrNull(routes: List<Route>): RoomId? {
    return routes.firstNotNullOfOrNull(::timelineRoomIdOrNull)
}

private fun timelineRoomIdOrNull(route: Route): RoomId? {
    return (route as? TimelineRoute)?.roomId
}
