package de.connect2x.trixnity.messenger.internal.compose.view.navigation

import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import de.connect2x.trixnity.messenger.internal.navigation.Route
import org.koin.core.module.Module
import org.koin.core.module.dsl.new

@TrixnityMessengerPrivateApi
inline fun <reified R : Route> Module.navigationOf(crossinline constructor: () -> NavigationEntry<R>) {
    navigation { new(constructor) }
}

@TrixnityMessengerPrivateApi
inline fun <reified R : Route, reified T1> Module.navigationOf(crossinline constructor: (T1) -> NavigationEntry<R>) {
    navigation { new(constructor) }
}
