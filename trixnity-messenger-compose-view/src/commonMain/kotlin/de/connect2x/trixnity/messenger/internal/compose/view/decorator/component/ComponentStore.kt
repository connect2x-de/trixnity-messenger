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
                factory =
                    viewModelFactory {
                        initializer<Wrapper<T>> { Wrapper(closeableComponentFactory.create(clazz, parameters)) }
                    },
            )

        return provider.get<Wrapper<T>>().component.value
    }
}

private class Wrapper<T>(val component: CloseableComponent<T>) : ViewModel(component)
