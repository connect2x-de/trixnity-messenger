package de.connect2x.trixnity.messenger.internal.compose.view.decorator.saveable

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.SaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.EntryDecoratorFactory

@TrixnityMessengerPrivateApi
interface SaveableStateHolderNavEntryDecoratorFactory : EntryDecoratorFactory {
    @Composable override fun <T : Any> create(): SaveableStateHolderNavEntryDecorator<T>
}

internal fun SaveableStateHolderNavEntryDecoratorFactory(): SaveableStateHolderNavEntryDecoratorFactory {
    return SaveableStateHolderNavEntryDecoratorFactoryImpl()
}

private class SaveableStateHolderNavEntryDecoratorFactoryImpl : SaveableStateHolderNavEntryDecoratorFactory {
    @Composable
    override fun <T : Any> create(): SaveableStateHolderNavEntryDecorator<T> {
        return rememberSaveableStateHolderNavEntryDecorator()
    }
}
