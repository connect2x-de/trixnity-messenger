package de.connect2x.trixnity.messenger.internal.compose.view.decorator.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreProvider
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import de.connect2x.trixnity.messenger.internal.component.CloseableComponentFactory

@TrixnityMessengerPrivateApi
@Composable
fun <T : Any> rememberComponentStoreNavEntryDecorator(
    closeableComponentFactory: CloseableComponentFactory
): ComponentStoreNavEntryDecorator<T> {
    val provider = rememberViewModelStoreProvider(key = closeableComponentFactory, parent = null)

    return remember(provider, closeableComponentFactory) {
        ComponentStoreNavEntryDecorator(provider = provider, factory = closeableComponentFactory)
    }
}
