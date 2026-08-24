package de.connect2x.trixnity.messenger.internal.compose.view.navigation

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import de.connect2x.trixnity.messenger.internal.navigation.Route
import kotlin.reflect.KClass
import kotlin.reflect.cast

@TrixnityMessengerPrivateApi
interface AnyNavigationEntry<R : Route> : NavigationEntry<R> {
    val clazz: KClass<out R>

    fun anyMetadata(route: Route): Map<String, Any> {
        return metadata(clazz.cast(route))
    }

    fun anyClazzContentKey(route: Route): Any {
        return clazzContentKey(clazz.cast(route))
    }

    @Composable
    fun AnyContent(route: Route) {
        Content(clazz.cast(route))
    }
}

@TrixnityMessengerPrivateApi
fun <R : Route> AnyNavigationEntry(clazz: KClass<out R>, entry: NavigationEntry<R>): AnyNavigationEntry<R> {
    return AnyNavigationEntryImpl(clazz = clazz, delegate = entry)
}

@TrixnityMessengerPrivateApi
inline fun <reified R : Route> AnyNavigationEntry(entry: NavigationEntry<R>): AnyNavigationEntry<R> {
    return AnyNavigationEntry(clazz = R::class, entry = entry)
}

private class AnyNavigationEntryImpl<R : Route>(override val clazz: KClass<out R>, delegate: NavigationEntry<R>) :
    AnyNavigationEntry<R>, NavigationEntry<R> by delegate
