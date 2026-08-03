package de.connect2x.trixnity.messenger.internal.workers

import de.connect2x.trixnity.messenger.Worker
import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.clear
import de.connect2x.trixnity.messenger.internal.navigation.replace
import de.connect2x.trixnity.messenger.internal.routes.ShareDataRoute
import de.connect2x.trixnity.messenger.util.SharedData
import de.connect2x.trixnity.messenger.util.SharedDataHandler

internal interface SharedDataWorker : Worker

internal fun SharedDataWorker(
    sharedDataHandler: SharedDataHandler,
    routeNavigation: RouteNavigation,
): SharedDataWorker {
    return SharedDataWorkerImpl(sharedDataHandler = sharedDataHandler, routeNavigation = routeNavigation)
}

private class SharedDataWorkerImpl(
    private val sharedDataHandler: SharedDataHandler,
    private val routeNavigation: RouteNavigation,
) : SharedDataWorker {

    override suspend fun doWork() {
        sharedDataHandler.collect(::updateNavigation)
    }

    private fun updateNavigation(data: SharedData?) {
        if (data == null) routeNavigation.updateNavigation { clear<ShareDataRoute>() }
        else routeNavigation.updateNavigation { replace<ShareDataRoute>(ShareDataRoute(data)) }
    }
}
