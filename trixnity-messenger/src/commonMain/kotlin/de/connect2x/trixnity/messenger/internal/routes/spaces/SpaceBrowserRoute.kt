package de.connect2x.trixnity.messenger.internal.routes.spaces

import androidx.compose.runtime.Immutable
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import kotlinx.serialization.Serializable

@TrixnityMessengerPrivateApi @Serializable @Immutable data object SpaceBrowserRoute : SpaceListRouteMarker
