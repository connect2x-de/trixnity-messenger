package de.connect2x.trixnity.messenger.internal.navigation

import de.connect2x.trixnity.messenger.internal.routes.root.MatrixClientInitializationRoute

internal fun DefaultInitialRoutes(): InitialRoutes {
    return InitialRoutes(initialRoutes = listOf(MatrixClientInitializationRoute))
}
