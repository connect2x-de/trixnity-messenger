package de.connect2x.trixnity.messenger.internal.routes

import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.internal.navigation.Route
import kotlinx.serialization.Serializable

@TrixnityMessengerCallApi @Serializable data class RoomCallRoute(val userId: UserId, val roomId: RoomId) : Route
