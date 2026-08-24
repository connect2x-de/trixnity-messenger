package de.connect2x.trixnity.messenger.internal.routes.root

import androidx.compose.runtime.Immutable
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.MatrixClientInitializationException
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import kotlinx.serialization.Serializable

@Serializable
@TrixnityMessengerPrivateApi
@Immutable
data class MatrixClientInitializationFailureRoute(
    val userId: UserId,
    val exception: MatrixClientInitializationException,
) : RootRouteMarker
