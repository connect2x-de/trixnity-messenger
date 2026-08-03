package de.connect2x.trixnity.messenger.internal.routes.root

import androidx.compose.runtime.Immutable
import de.connect2x.trixnity.clientserverapi.client.oauth2.OAuth2AuthorizationCodeLoginFlow
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import de.connect2x.trixnity.messenger.viewmodel.connecting.OAuth2AuthorizationCodeLoginViewModel
import kotlinx.serialization.Serializable

@Serializable
@TrixnityMessengerPrivateApi
@Immutable
data class OAuth2AuthorizationCodeLoginRoute(
    val serverUrl: String,
    val kind: OAuth2AuthorizationCodeLoginViewModel.Type,
    val initialState: OAuth2AuthorizationCodeLoginFlow.AuthRequestData.State? = null,
    val redirectUri: String? = null,
) : RootRouteMarker
