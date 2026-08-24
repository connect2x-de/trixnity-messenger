package de.connect2x.trixnity.messenger.internal.routes

import androidx.compose.runtime.Immutable
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import de.connect2x.trixnity.messenger.internal.navigation.Route
import de.connect2x.trixnity.messenger.util.SharedData
import kotlinx.serialization.Serializable

@TrixnityMessengerPrivateApi @Serializable @Immutable data class ShareDataRoute(val data: SharedData) : Route
