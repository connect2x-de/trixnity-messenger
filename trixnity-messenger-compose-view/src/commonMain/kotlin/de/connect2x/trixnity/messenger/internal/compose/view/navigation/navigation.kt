package de.connect2x.trixnity.messenger.internal.compose.view.navigation

import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import de.connect2x.trixnity.messenger.internal.navigation.Route
import org.koin.core.definition.Definition
import org.koin.core.definition.KoinDefinition
import org.koin.core.module.Module
import org.koin.core.qualifier.named

@TrixnityMessengerPrivateApi
inline fun <reified T : Route> Module.navigation(
    noinline definition: Definition<NavigationEntry<T>>
): KoinDefinition<AnyNavigationEntry<T>> {
    single<NavigationEntry<T>>(qualifier = named<T>(), definition = definition)

    return single<AnyNavigationEntry<T>>(named<T>()) { AnyNavigationEntry(get<NavigationEntry<T>>(named<T>())) }
}
