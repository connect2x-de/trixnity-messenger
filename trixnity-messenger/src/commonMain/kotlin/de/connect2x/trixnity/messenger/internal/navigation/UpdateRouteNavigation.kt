package de.connect2x.trixnity.messenger.internal.navigation

import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi

@TrixnityMessengerPrivateApi
interface UpdateRouteNavigation {
    fun updateNavigation(transform: RouteNavigationScope.() -> Unit)
}
