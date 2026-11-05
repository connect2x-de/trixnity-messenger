package de.connect2x.trixnity.messenger.internal.routes.selfverification

import androidx.compose.runtime.Immutable
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import kotlinx.serialization.Serializable

@TrixnityMessengerPrivateApi
@Immutable
@Serializable
data class CrossSigningBootstrapRecoveryKeyRoute(val userId: UserId) : CrossSigningBootstrapStepRouteMarker
