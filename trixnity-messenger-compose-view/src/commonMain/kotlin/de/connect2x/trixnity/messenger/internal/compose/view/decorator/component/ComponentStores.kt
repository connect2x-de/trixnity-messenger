package de.connect2x.trixnity.messenger.internal.compose.view.decorator.component

import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi

@TrixnityMessengerPrivateApi
interface ComponentStores {
    fun forKey(key: ComponentStoreKey): ComponentStore
}

internal fun ComponentStores(stores: Map<ComponentStoreKey, ComponentStore>): ComponentStores {
    return ComponentStoresImpl(stores = stores)
}

private class ComponentStoresImpl(private val stores: Map<ComponentStoreKey, ComponentStore>) : ComponentStores {
    override fun forKey(key: ComponentStoreKey): ComponentStore {
        return checkNotNull(stores[key]) { "no store for $key" }
    }
}
