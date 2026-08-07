package de.connect2x.trixnity.messenger.internal.compose.view.decorator.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.ViewModelStoreProvider
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreOwner
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import de.connect2x.trixnity.messenger.internal.component.CloseableComponentFactory

@TrixnityMessengerPrivateApi
class ComponentStoreNavEntryDecorator<T : Any>(
    private val provider: ViewModelStoreProvider,
    private val factory: CloseableComponentFactory,
) :
    NavEntryDecorator<T>(
        onPop = provider::clearKey,
        decorate = { entry -> ComponentStoreNavEntry(provider = provider, factory = factory, entry = entry).Content() },
    ) {

    @TrixnityMessengerPrivateApi
    companion object {
        fun <T : Any> context(key: ComponentStoreKey, route: T): ComponentStoreContext {
            return ComponentStoreContext(key = key, route = route)
        }

        fun context(key: ComponentStoreKey, contentKey: Any, parameters: Set<Any?>): ComponentStoreContext {
            return ComponentStoreContext(key = key, contentKey = contentKey, parameters = parameters)
        }

        operator fun Map<String, Any>.plus(componentStoreContext: ComponentStoreContext): Map<String, Any> {
            val context = get(ComponentStoreNavEntryDecoratorContextMetadataKey).orEmpty() + componentStoreContext
            return this + metadata { put(key = ComponentStoreNavEntryDecoratorContextMetadataKey, value = context) }
        }
    }

    @TrixnityMessengerPrivateApi
    data object ComponentStoreNavEntryDecoratorContextMetadataKey : NavMetadataKey<List<ComponentStoreContext>>
}

private fun <T : Any> ComponentStoreNavEntry(
    provider: ViewModelStoreProvider,
    factory: CloseableComponentFactory,
    entry: NavEntry<T>,
): NavEntry<T> =
    NavEntry(navEntry = entry) { route ->
        val componentStores =
            rememberComponentStores(
                contexts = componentStoreContexts(entry = entry, route = route),
                provider = provider,
                factory = factory,
            )

        CompositionLocalProvider(LocalComponentStores provides componentStores) { entry.Content() }
    }

private fun <T : Any> componentStoreContexts(entry: NavEntry<T>, route: T): List<ComponentStoreContext> {
    return listOf(
        ComponentStoreContext(key = CurrentComponentStoreKey, contentKey = entry.contentKey, parameters = setOf(route))
    ) + entry.metadata[ComponentStoreNavEntryDecorator.ComponentStoreNavEntryDecoratorContextMetadataKey].orEmpty()
}

@Composable
private fun rememberComponentStores(
    contexts: List<ComponentStoreContext>,
    provider: ViewModelStoreProvider,
    factory: CloseableComponentFactory,
): ComponentStores {
    val stores = contexts.associate { context ->
        context.key to rememberComponentStore(context = context, provider = provider, factory = factory)
    }

    return remember(stores) { ComponentStores(stores = stores) }
}

@Composable
private fun rememberComponentStore(
    context: ComponentStoreContext,
    provider: ViewModelStoreProvider,
    factory: CloseableComponentFactory,
): ComponentStore {
    val viewModelStoreOwner = rememberViewModelStoreOwner(key = context.contentKey, provider = provider)

    return remember(viewModelStoreOwner, factory, context) {
        ComponentStore(
            viewModelStoreOwner = viewModelStoreOwner,
            closeableComponentFactory = factory,
            parameters = context.parameters,
        )
    }
}
