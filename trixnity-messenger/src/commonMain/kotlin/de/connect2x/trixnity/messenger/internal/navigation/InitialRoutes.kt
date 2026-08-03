package de.connect2x.trixnity.messenger.internal.navigation

import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi

@TrixnityMessengerPrivateApi
interface InitialRoutes {
    val routes: List<Route>
}

@TrixnityMessengerPrivateApi
fun InitialRoutes(initialRoutes: List<Route>): InitialRoutes {
    return InitialRoutesImpl(routes = initialRoutes)
}

private class InitialRoutesImpl(override val routes: List<Route>) : InitialRoutes
