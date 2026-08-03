package de.connect2x.trixnity.messenger.internal.logic

import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.util.FileDescriptor

internal interface SendAttachmentLogic {
    suspend fun showAttachmentSendView(userId: UserId, roomId: RoomId, file: FileDescriptor)
}
