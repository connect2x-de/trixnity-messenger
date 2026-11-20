package de.connect2x.trixnity.messenger.internal.routes.spaces

import androidx.compose.runtime.Immutable
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import de.connect2x.trixnity.messenger.internal.routes.extras.ExtrasRouteMarker
import kotlinx.serialization.Serializable

@TrixnityMessengerPrivateApi
@Serializable
@Immutable
data class SpaceSettingsRoute(val userId: UserId, val spaceId: RoomId) : ExtrasRouteMarker
