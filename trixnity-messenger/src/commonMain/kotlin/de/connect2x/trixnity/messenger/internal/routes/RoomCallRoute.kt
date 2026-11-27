package de.connect2x.trixnity.messenger.internal.routes

import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.call.CallTarget
import de.connect2x.trixnity.messenger.internal.routes.extras.ExtrasRouteMarker
import kotlinx.serialization.Serializable

@TrixnityMessengerCallApi @Serializable data class RoomCallRoute(val target: CallTarget) : ExtrasRouteMarker
