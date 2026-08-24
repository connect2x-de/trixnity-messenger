package de.connect2x.trixnity.messenger.internal.navigation

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import io.ktor.util.reflect.instanceOf
import kotlin.reflect.KClass

@TrixnityMessengerPrivateApi
interface RouteNavigationScope {
    var items: List<Route>

    fun <T : Route> replace(route: T, clazz: KClass<T>)

    fun <T : Route> clear(clazz: KClass<T>)

    fun push(route: Route)

    fun pop(route: Route)
}

@TrixnityMessengerPrivateApi
fun RouteNavigationScope(items: List<Route>): RouteNavigationScope {
    return RouteNavigationScopeImpl(items)
}

private class RouteNavigationScopeImpl(override var items: List<Route>) : RouteNavigationScope {
    override fun <T : Route> replace(route: T, clazz: KClass<T>) {
        clear(clazz)
        push(route)
    }

    override fun <T : Route> clear(clazz: KClass<T>) {
        log.trace { "clear($clazz)" }
        items = items.filterNot { it.instanceOf(clazz) }
    }

    override fun push(route: Route) {
        log.trace { "push($route)" }
        items = items + route
    }

    override fun pop(route: Route) {
        log.trace { "pop($route)" }
        items = items - route
    }

    companion object {
        val log = Logger("de.connect2x.trixnity.messenger.internal.navigation.RouteNavigationScope")
    }
}
