package de.connect2x.trixnity.messenger.internal.compose.view.decorator.component

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import kotlin.reflect.KClass

@TrixnityMessengerPrivateApi
@Composable
fun <T : Any> rememberComponent(
    clazz: KClass<out T>,
    key: ComponentStoreKey = CurrentComponentStoreKey,
    stores: ComponentStores = LocalComponentStores.current,
): T {
    return stores.forKey(key).getOrCreate(clazz)
}

@TrixnityMessengerPrivateApi
@Composable
inline fun <reified T : Any> rememberComponent(
    key: ComponentStoreKey = CurrentComponentStoreKey,
    stores: ComponentStores = LocalComponentStores.current,
): T {
    return rememberComponent(clazz = T::class, key = key, stores = stores)
}
