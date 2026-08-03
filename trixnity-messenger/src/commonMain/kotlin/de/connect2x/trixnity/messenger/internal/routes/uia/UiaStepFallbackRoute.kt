package de.connect2x.trixnity.messenger.internal.routes.uia

import androidx.compose.runtime.Immutable
import de.connect2x.trixnity.clientserverapi.model.uia.AuthenticationType
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import kotlinx.serialization.Serializable

@TrixnityMessengerPrivateApi
@Serializable
@Immutable
data class UiaStepFallbackRoute(val authenticationType: AuthenticationType) : UiaRouteMarker
