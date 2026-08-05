package de.connect2x.trixnity.messenger.internal.compose.view.navigation

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import de.connect2x.trixnity.messenger.internal.navigation.Route

@TrixnityMessengerPrivateApi
interface NavigationEntry<R : Route> {

    val metadata: Map<String, Any>
        get() = emptyMap()

    fun metadata(route: R): Map<String, Any> {
        return metadata
    }

    fun clazzContentKey(route: R): Any {
        return route.toString()
    }

    @Composable fun Content(route: R)
}
