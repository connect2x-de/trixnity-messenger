package de.connect2x.trixnity.messenger.internal.routes.root

import androidx.compose.runtime.Immutable
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import kotlinx.serialization.Serializable

@Serializable
@TrixnityMessengerPrivateApi
@Immutable
data class SSOLoginRoute(
    val serverUrl: String,
    val providerId: String?,
    val providerName: String?,
    val initialState: String? = null,
    val redirectUri: String? = null,
) : RootRouteMarker
