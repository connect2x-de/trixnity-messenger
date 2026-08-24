package de.connect2x.trixnity.messenger.internal.logic

import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.routes.SendAttachmentRoute
import de.connect2x.trixnity.messenger.util.FileDescriptor

internal interface SendAttachmentLogic {
    suspend fun showAttachmentSendView(userId: UserId, roomId: RoomId, file: FileDescriptor)
}

internal fun SendAttachmentLogic(routeNavigation: RouteNavigation): SendAttachmentLogic {
    return SendAttachmentLogicImpl(routeNavigation = routeNavigation)
}

private class SendAttachmentLogicImpl(private val routeNavigation: RouteNavigation) : SendAttachmentLogic {
    override suspend fun showAttachmentSendView(userId: UserId, roomId: RoomId, file: FileDescriptor) {
        routeNavigation.updateNavigation { push(SendAttachmentRoute(userId, roomId, file)) }
    }
}
