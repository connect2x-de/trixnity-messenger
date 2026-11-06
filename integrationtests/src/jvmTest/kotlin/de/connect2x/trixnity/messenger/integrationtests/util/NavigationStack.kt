package de.connect2x.trixnity.messenger.integrationtests.util

import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import de.connect2x.trixnity.messenger.integrationtests.messenger.nav3.Nav3MatrixMessengerWithRoot
import de.connect2x.trixnity.messenger.internal.navigation.Route
import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation

@OptIn(TrixnityMessengerPrivateApi::class)
fun Nav3MatrixMessengerWithRoot.push(route: Route) {
    di.get<RouteNavigation>().updateNavigation { push(route) }
}
