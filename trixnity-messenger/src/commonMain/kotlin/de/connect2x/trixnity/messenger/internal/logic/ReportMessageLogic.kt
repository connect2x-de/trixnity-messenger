package de.connect2x.trixnity.messenger.internal.logic

import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.routes.ReportMessageRoute

internal interface ReportMessageLogic {
    suspend fun showReportMessageDialog(userId: UserId, roomId: RoomId, eventId: EventId)
}

internal fun ReportMessageLogic(routeNavigation: RouteNavigation): ReportMessageLogic {
    return ReportMessageLogicImpl(routeNavigation = routeNavigation)
}

private class ReportMessageLogicImpl(private val routeNavigation: RouteNavigation) : ReportMessageLogic {
    override suspend fun showReportMessageDialog(userId: UserId, roomId: RoomId, eventId: EventId) {
        routeNavigation.updateNavigation { push(ReportMessageRoute(userId, roomId, eventId)) }
    }
}
