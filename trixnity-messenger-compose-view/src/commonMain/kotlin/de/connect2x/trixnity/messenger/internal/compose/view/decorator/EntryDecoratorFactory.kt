package de.connect2x.trixnity.messenger.internal.compose.view.decorator

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntryDecorator
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import de.connect2x.trixnity.messenger.internal.sort.SortableScope
import de.connect2x.trixnity.messenger.internal.sort.sorted
import org.koin.core.definition.Definition
import org.koin.core.definition.KoinDefinition
import org.koin.core.module.Module
import org.koin.core.module.dsl.new
import org.koin.dsl.bind

@TrixnityMessengerPrivateApi
interface EntryDecoratorFactory {
    @Composable fun <T : Any> create(): NavEntryDecorator<T>
}

@TrixnityMessengerPrivateApi
inline fun <reified T : EntryDecoratorFactory> Module.entryDecorator(
    noinline definition: Definition<T>,
    noinline configure: SortableScope<EntryDecoratorFactory>.() -> Unit = {},
): KoinDefinition<out EntryDecoratorFactory> {
    return single<T>(definition = definition).bind<EntryDecoratorFactory>().sorted(configure)
}

@TrixnityMessengerPrivateApi
inline fun <reified T : EntryDecoratorFactory> Module.entryDecoratorOf(
    noinline constructor: () -> T,
    noinline configure: SortableScope<EntryDecoratorFactory>.() -> Unit = {},
): KoinDefinition<out EntryDecoratorFactory> {
    return entryDecorator(definition = { new(constructor) }, configure = configure)
}

@TrixnityMessengerPrivateApi
inline fun <reified T : EntryDecoratorFactory, reified T1> Module.entryDecoratorOf(
    noinline constructor: (T1) -> T,
    noinline configure: SortableScope<EntryDecoratorFactory>.() -> Unit = {},
): KoinDefinition<out EntryDecoratorFactory> {
    return entryDecorator(definition = { new(constructor) }, configure = configure)
}
