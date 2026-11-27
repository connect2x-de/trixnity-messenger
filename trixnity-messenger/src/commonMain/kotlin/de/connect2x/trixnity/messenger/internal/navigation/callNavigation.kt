package de.connect2x.trixnity.messenger.internal.navigation

import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.call.CallTarget
import de.connect2x.trixnity.messenger.internal.routes.RoomCallRoute
import de.connect2x.trixnity.messenger.internal.routes.roomlist.RoomListRoute

@TrixnityMessengerCallApi
fun RouteNavigationScope.restoreCall(target: CallTarget) {
    replace(RoomCallRoute(target))
}

@TrixnityMessengerCallApi
fun RouteNavigationScope.removeCall(target: CallTarget) {
    items = items.filterNot { it is RoomCallRoute && it.target == target }.ifEmpty { listOf(RoomListRoute) }
}
