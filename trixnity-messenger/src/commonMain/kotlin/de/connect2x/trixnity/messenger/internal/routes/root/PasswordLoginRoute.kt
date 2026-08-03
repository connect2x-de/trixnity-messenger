package de.connect2x.trixnity.messenger.internal.routes.root

import androidx.compose.runtime.Immutable
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import kotlinx.serialization.Serializable

@TrixnityMessengerPrivateApi
@Serializable
@Immutable
data class PasswordLoginRoute(val serverUrl: String) : RootRouteMarker
