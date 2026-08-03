package de.connect2x.trixnity.messenger.internal.navigation

import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import org.koin.core.definition.Definition
import org.koin.core.module.Module
import org.koin.core.module.dsl.new
import org.koin.core.qualifier.named

@TrixnityMessengerPrivateApi
inline fun <reified V : Any> Module.viewModelFactoryAdapter(
    noinline definition: Definition<ViewModelFactoryAdapter<V>>
) {
    val qualifier = named<V>()

    single<ViewModelFactoryAdapter<V>>(qualifier = qualifier, definition = definition)

    factory<V> { parameters -> get<ViewModelFactoryAdapter<V>>(qualifier = qualifier).create(parameters) }
}

@TrixnityMessengerPrivateApi
inline fun <reified V : Any> Module.viewModelFactoryAdapterOf(
    crossinline constructor: () -> ViewModelFactoryAdapter<V>
) = viewModelFactoryAdapter { new(constructor) }

@TrixnityMessengerPrivateApi
inline fun <reified V : Any, reified T1 : Any> Module.viewModelFactoryAdapterOf(
    crossinline constructor: (T1) -> ViewModelFactoryAdapter<V>
) = viewModelFactoryAdapter { new(constructor) }

@TrixnityMessengerPrivateApi
inline fun <reified V : Any, reified T1 : Any, reified T2 : Any> Module.viewModelFactoryAdapterOf(
    crossinline constructor: (T1, T2) -> ViewModelFactoryAdapter<V>
) = viewModelFactoryAdapter { new(constructor) }

@TrixnityMessengerPrivateApi
inline fun <reified V : Any, reified T1 : Any, reified T2 : Any, reified T3 : Any> Module.viewModelFactoryAdapterOf(
    crossinline constructor: (T1, T2, T3) -> ViewModelFactoryAdapter<V>
) = viewModelFactoryAdapter { new(constructor) }

@TrixnityMessengerPrivateApi
inline fun <reified V : Any, reified T1 : Any, reified T2 : Any, reified T3 : Any, reified T4 : Any> Module
    .viewModelFactoryAdapterOf(crossinline constructor: (T1, T2, T3, T4) -> ViewModelFactoryAdapter<V>) =
    viewModelFactoryAdapter {
        new(constructor)
    }

@TrixnityMessengerPrivateApi
inline fun <
    reified V : Any,
    reified T1 : Any,
    reified T2 : Any,
    reified T3 : Any,
    reified T4 : Any,
    reified T5 : Any,
> Module.viewModelFactoryAdapterOf(crossinline constructor: (T1, T2, T3, T4, T5) -> ViewModelFactoryAdapter<V>) =
    viewModelFactoryAdapter {
        new(constructor)
    }

@TrixnityMessengerPrivateApi
inline fun <
    reified V : Any,
    reified T1 : Any,
    reified T2 : Any,
    reified T3 : Any,
    reified T4 : Any,
    reified T5 : Any,
    reified T6 : Any,
> Module.viewModelFactoryAdapterOf(crossinline constructor: (T1, T2, T3, T4, T5, T6) -> ViewModelFactoryAdapter<V>) =
    viewModelFactoryAdapter {
        new(constructor)
    }
