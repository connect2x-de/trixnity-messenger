package de.connect2x.trixnity.messenger.internal.compose.view.decorator.component

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import de.connect2x.trixnity.messenger.internal.component.CloseableComponentFactory
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.EntryDecoratorFactory

@TrixnityMessengerPrivateApi
interface ComponentStoreNavEntryDecoratorFactory : EntryDecoratorFactory {
    @Composable override fun <T : Any> create(): ComponentStoreNavEntryDecorator<T>
}

internal fun ComponentStoreNavEntryDecoratorFactory(
    closeableComponentFactory: CloseableComponentFactory
): ComponentStoreNavEntryDecoratorFactory {
    return ComponentStoreNavEntryDecoratorFactoryImpl(closeableComponentFactory = closeableComponentFactory)
}

private class ComponentStoreNavEntryDecoratorFactoryImpl(
    private val closeableComponentFactory: CloseableComponentFactory
) : ComponentStoreNavEntryDecoratorFactory {
    @Composable
    override fun <T : Any> create(): ComponentStoreNavEntryDecorator<T> {
        return rememberComponentStoreNavEntryDecorator(closeableComponentFactory = closeableComponentFactory)
    }
}
