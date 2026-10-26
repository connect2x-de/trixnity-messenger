package de.connect2x.trixnity.messenger.internal.compose.view.decorator.component

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.get
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import de.connect2x.trixnity.messenger.internal.component.CloseableComponent
import de.connect2x.trixnity.messenger.internal.component.CloseableComponentFactory
import kotlin.reflect.KClass

@TrixnityMessengerPrivateApi
interface ComponentStore {
    fun <T : Any> getOrCreate(clazz: KClass<out T>): T
}

internal fun ComponentStore(
    viewModelStoreOwner: ViewModelStoreOwner,
    closeableComponentFactory: CloseableComponentFactory,
    parameters: Set<Any?>,
): ComponentStore {
    return ComponentStoreImpl(
        viewModelStoreOwner = viewModelStoreOwner,
        closeableComponentFactory = closeableComponentFactory,
        parameters = parameters,
    )
}

private class ComponentStoreImpl(
    private val viewModelStoreOwner: ViewModelStoreOwner,
    private val closeableComponentFactory: CloseableComponentFactory,
    private val parameters: Set<Any?>,
) : ComponentStore {
    override fun <T : Any> getOrCreate(clazz: KClass<out T>): T {
        val provider =
            ViewModelProvider.create(
                store = viewModelStoreOwner.viewModelStore,
                factory = viewModelFactory { initializer { Components() } },
            )

        return provider.get<Components>().getOrCreate(clazz) { closeableComponentFactory.create(clazz, parameters) }
    }
}

private class Components : ViewModel() {
    private val components = mutableMapOf<KClass<*>, CloseableComponent<out Any>>()

    fun <T : Any> getOrCreate(clazz: KClass<out T>, create: () -> CloseableComponent<T>): T {
        val component = components.getOrPut(clazz) { create().also { addCloseable(it) } }
        @Suppress("UNCHECKED_CAST")
        return component.value as T
    }
}
