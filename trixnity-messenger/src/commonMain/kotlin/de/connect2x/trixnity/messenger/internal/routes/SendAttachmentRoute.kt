package de.connect2x.trixnity.messenger.internal.routes

import androidx.compose.runtime.Immutable
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import de.connect2x.trixnity.messenger.internal.navigation.Route
import de.connect2x.trixnity.messenger.util.FileDescriptor
import kotlinx.serialization.Serializable

@TrixnityMessengerPrivateApi
@Serializable
@Immutable
data class SendAttachmentRoute(val userId: UserId, val roomId: RoomId, val fileDescriptor: FileDescriptor) : Route
