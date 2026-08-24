package de.connect2x.trixnity.messenger.internal.routes.roomlist

import androidx.compose.runtime.Immutable
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import kotlinx.serialization.Serializable

@TrixnityMessengerPrivateApi @Serializable @Immutable data object NotificationSettingsRoute : RoomListRouteMarker
