package de.connect2x.trixnity.messenger.internal.routes.root

import androidx.compose.runtime.Immutable
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import kotlinx.serialization.Serializable

@Serializable
@TrixnityMessengerPrivateApi
@Immutable
data class OAuth2DeviceAuthorizationLoginRoute(val serverUrl: String) : RootRouteMarker
