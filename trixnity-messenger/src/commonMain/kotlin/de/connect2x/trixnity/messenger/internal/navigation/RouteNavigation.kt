package de.connect2x.trixnity.messenger.internal.navigation

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@TrixnityMessengerPrivateApi interface RouteNavigation : GetRouteNavigation, UpdateRouteNavigation

@TrixnityMessengerPrivateApi
fun RouteNavigation(initialRoutes: InitialRoutes): RouteNavigation {
    return RouteNavigationImpl(initialRoutes = initialRoutes.routes)
}

private class RouteNavigationImpl(initialRoutes: List<Route>) : RouteNavigation {

    private val _routes = MutableStateFlow(initialRoutes)

    override val routes: StateFlow<List<Route>> = _routes.asStateFlow()

    override fun updateNavigation(transform: RouteNavigationScope.() -> Unit) {
        _routes.update { itemsBefore ->
            val scope = RouteNavigationScope(itemsBefore)
            scope.transform()
            val itemsAfter = scope.items

            log.trace { "updateNavigation(before=$itemsBefore, after=$itemsAfter" }

            itemsAfter
        }
    }

    companion object {
        val log = Logger("de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation")
    }
}
