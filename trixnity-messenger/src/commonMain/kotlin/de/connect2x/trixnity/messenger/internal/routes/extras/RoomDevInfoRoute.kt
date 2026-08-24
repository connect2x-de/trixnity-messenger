package de.connect2x.trixnity.messenger.internal.routes.extras

import androidx.compose.runtime.Immutable
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import kotlinx.serialization.Serializable

@TrixnityMessengerPrivateApi
@Serializable
@Immutable
data class RoomDevInfoRoute(val userId: UserId, val roomId: RoomId) : ExtrasRouteMarker
