@file:OptIn(ExperimentalDecomposeApi::class)

package de.connect2x.trixnity.messenger.internal.navigation

import com.arkivanov.decompose.ExperimentalDecomposeApi
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi

@TrixnityMessengerPrivateApi
fun RouteNavigation.navigationCallback(transform: RouteNavigationScope.() -> Unit): () -> Unit = {
    updateNavigation { transform() }
}

@TrixnityMessengerPrivateApi
fun <T1> RouteNavigation.navigationCallback(transform: RouteNavigationScope.(T1) -> Unit): (T1) -> Unit = { t1 ->
    updateNavigation { transform(t1) }
}

@TrixnityMessengerPrivateApi
fun <T1, T2> RouteNavigation.navigationCallback(transform: RouteNavigationScope.(T1, T2) -> Unit): (T1, T2) -> Unit =
    { t1, t2 ->
        updateNavigation { transform(t1, t2) }
    }

@TrixnityMessengerPrivateApi
fun <T1, T2, T3> RouteNavigation.navigationCallback(
    transform: RouteNavigationScope.(T1, T2, T3) -> Unit
): (T1, T2, T3) -> Unit = { t1, t2, t3 -> updateNavigation { transform(t1, t2, t3) } }
