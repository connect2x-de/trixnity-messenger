package de.connect2x.trixnity.messenger.internal.compose.view.decorator.component

import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi

@TrixnityMessengerPrivateApi
data class ComponentStoreContext(val key: ComponentStoreKey, val contentKey: Any, val parameters: Set<Any?>)

@TrixnityMessengerPrivateApi
fun <T : Any> ComponentStoreContext(key: ComponentStoreKey, route: T): ComponentStoreContext {
    return ComponentStoreContext(key = key, contentKey = route.toString(), parameters = setOf(route))
}
