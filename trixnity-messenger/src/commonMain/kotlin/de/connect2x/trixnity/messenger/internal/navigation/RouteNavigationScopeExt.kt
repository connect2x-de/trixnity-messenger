package de.connect2x.trixnity.messenger.internal.navigation

import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi

@TrixnityMessengerPrivateApi
inline fun <reified T : Route> RouteNavigationScope.replace(route: T) {
    replace(route, T::class)
}

@TrixnityMessengerPrivateApi
inline fun <reified T : Route> RouteNavigationScope.clear() {
    clear(T::class)
}

@TrixnityMessengerPrivateApi
inline fun <reified T : Route> RouteNavigationScope.push(route: T) {
    push(route)
}

@TrixnityMessengerPrivateApi
inline fun <reified T : Route> RouteNavigationScope.pop(route: T) {
    pop(route)
}
