package de.connect2x.trixnity.messenger.internal.workers

import de.connect2x.trixnity.messenger.Worker
import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.routes.roomlist.RoomListRoute
import de.connect2x.trixnity.messenger.notification.NotificationHandlers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal interface RequestNotificationPermissionsWorker : Worker

internal fun RequestNotificationPermissionsWorker(
    notificationHandlers: NotificationHandlers,
    routeNavigation: RouteNavigation,
): RequestNotificationPermissionsWorker {
    return RequestNotificationPermissionsWorkerImpl(
        notificationHandlers = notificationHandlers,
        routeNavigation = routeNavigation,
    )
}

private class RequestNotificationPermissionsWorkerImpl(
    private val notificationHandlers: NotificationHandlers,
    private val routeNavigation: RouteNavigation,
) : RequestNotificationPermissionsWorker {
    override suspend fun doWork() {
        isRequestNotificationPermissionsAllowed(routeNavigation).collectLatest { isAllowed ->
            if (isAllowed) notificationHandlers.continuouslyRequestPermissions()
        }
    }
}

private fun isRequestNotificationPermissionsAllowed(routeNavigation: RouteNavigation): Flow<Boolean> {
    return routeNavigation.routes.map { it.filterIsInstance<RoomListRoute>().isNotEmpty() }.distinctUntilChanged()
}
