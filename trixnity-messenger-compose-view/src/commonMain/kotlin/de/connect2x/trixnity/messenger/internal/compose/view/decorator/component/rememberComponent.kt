package de.connect2x.trixnity.messenger.internal.compose.view.decorator.component

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import kotlin.reflect.KClass

@TrixnityMessengerPrivateApi
@Composable
fun <T : Any> rememberComponent(clazz: KClass<out T>, stores: ComponentStores = LocalComponentStores.current): T {
    return rememberComponentForKey(clazz, CurrentComponentStoreKey, stores)
}

@TrixnityMessengerPrivateApi
@Composable
fun <T : Any> rememberComponentForKey(
    clazz: KClass<out T>,
    key: ComponentStoreKey,
    stores: ComponentStores = LocalComponentStores.current,
): T {
    return stores.forKey(key).getOrCreate(clazz)
}

@TrixnityMessengerPrivateApi
@Composable
inline fun <reified T : Any> rememberComponent(stores: ComponentStores = LocalComponentStores.current): T {
    return rememberComponent(clazz = T::class, stores = stores)
}

/**
 * Remembers a component via a [ComponentStoreKey], allowing for sharing of components between entries when used with a
 * [ComponentStoreContext]
 *
 * @param key the [ComponentStoreKey] that is used to access the shared components
 */
@TrixnityMessengerPrivateApi
@Composable
inline fun <reified T : Any> rememberComponentForKey(
    key: ComponentStoreKey,
    stores: ComponentStores = LocalComponentStores.current,
): T {
    return rememberComponentForKey(clazz = T::class, key = key, stores = stores)
}
