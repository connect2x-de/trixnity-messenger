package de.connect2x.trixnity.messenger.internal.logic

import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId

internal interface ReportMessageLogic {
    suspend fun showReportMessageDialog(userId: UserId, roomId: RoomId, eventId: EventId)
}
