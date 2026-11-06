package de.connect2x.trixnity.messenger.internal.routes.verification

import androidx.compose.runtime.Immutable
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import kotlinx.serialization.Serializable

@TrixnityMessengerPrivateApi
@Serializable
@Immutable
data class VerificationRequestRoute(val userId: UserId) : VerificationStepRouteMarker
