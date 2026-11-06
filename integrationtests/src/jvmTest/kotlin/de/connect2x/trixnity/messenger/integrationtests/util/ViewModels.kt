package de.connect2x.trixnity.messenger.integrationtests.util

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import de.connect2x.trixnity.messenger.integrationtests.messenger.nav3.Nav3MatrixMessengerWithRoot
import de.connect2x.trixnity.messenger.internal.component.CloseableComponentFactory
import de.connect2x.trixnity.messenger.internal.navigation.GetRouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.Route
import de.connect2x.trixnity.messenger.viewmodel.util.toFlow
import io.ktor.util.reflect.*
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.first

private val log: Logger = Logger("de.connect2x.trixnity.messenger.integrationtests.util.ViewModelsKt")

@Suppress("UNCHECKED_CAST")
suspend fun <C : Any, W : Any, T : Any> Value<ChildStack<C, W>>.waitFor(clazz: KClass<T>): T {
    return this.toFlow()
        .first { childStack ->
            log.debug { "wait for $clazz, active: ${childStack.active.instance}" }
            childStack.active.instance.instanceOf(clazz)
        }
        .active
        .instance as T
}

@OptIn(TrixnityMessengerPrivateApi::class)
@Suppress("UNCHECKED_CAST")
suspend fun <T : Route> Nav3MatrixMessengerWithRoot.waitFor(clazz: KClass<T>): T {
    val stack = di.get<GetRouteNavigation>()
    return stack.routes
        .first { stack ->
            log.debug { "wait for $clazz, active: ${stack.last()}, backStack: $stack" }
            stack.last().instanceOf(clazz)
        }
        .last() as T
}

@OptIn(TrixnityMessengerPrivateApi::class)
@Suppress("UNCHECKED_CAST")
suspend inline fun <reified T : Route> Nav3MatrixMessengerWithRoot.waitFor(): T {
    return waitFor(T::class)
}

@OptIn(TrixnityMessengerPrivateApi::class)
@Suppress("UNCHECKED_CAST")
suspend inline fun <reified T : Route, reified V : Any> Nav3MatrixMessengerWithRoot.waitForViewModel(): V {
    val route = waitFor(T::class)

    return createViewModelFromAdapter<V>(route)
}

@OptIn(TrixnityMessengerPrivateApi::class)
inline fun <reified V : Any> Nav3MatrixMessengerWithRoot.createViewModelFromAdapter(route: Route): V {
    return di.get<CloseableComponentFactory>().create(V::class, setOf(route)).value
}

@OptIn(TrixnityMessengerPrivateApi::class)
@Suppress("UNCHECKED_CAST")
suspend fun <T : Route> Nav3MatrixMessengerWithRoot.waitForNotActive(clazz: KClass<T>) {
    val stack = di.get<GetRouteNavigation>()
    stack.routes.first { childStack ->
        log.debug { "wait for $clazz, active: ${childStack.last()}, backStack: $childStack" }
        childStack.last().instanceOf(clazz).not()
    }
}

@OptIn(TrixnityMessengerPrivateApi::class)
@Suppress("UNCHECKED_CAST")
suspend inline fun <reified T : Route> Nav3MatrixMessengerWithRoot.waitForNotActive() {
    waitForNotActive(T::class)
}
