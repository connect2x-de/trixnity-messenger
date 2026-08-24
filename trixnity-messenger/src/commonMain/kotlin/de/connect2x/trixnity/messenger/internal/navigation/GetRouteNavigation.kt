package de.connect2x.trixnity.messenger.internal.navigation

import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import kotlinx.coroutines.flow.StateFlow

@TrixnityMessengerPrivateApi
interface GetRouteNavigation {
    val routes: StateFlow<List<Route>>
}
