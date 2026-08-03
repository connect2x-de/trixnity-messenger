package de.connect2x.trixnity.messenger.internal.workers

import de.connect2x.trixnity.client.verification.ActiveVerificationState
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.Worker
import de.connect2x.trixnity.messenger.internal.logic.ActiveDeviceVerificationStatesLogic
import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.routes.VerificationRoute
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

internal interface DeviceVerificationWorker : Worker

internal fun DeviceVerificationWorker(
    activeDeviceVerificationStatesLogic: ActiveDeviceVerificationStatesLogic,
    routeNavigation: RouteNavigation,
): DeviceVerificationWorker {
    return DeviceVerificationWorkerImpl(
        activeDeviceVerificationStatesLogic = activeDeviceVerificationStatesLogic,
        routeNavigation = routeNavigation,
    )
}

private class DeviceVerificationWorkerImpl(
    private val activeDeviceVerificationStatesLogic: ActiveDeviceVerificationStatesLogic,
    private val routeNavigation: RouteNavigation,
) : DeviceVerificationWorker {

    override suspend fun doWork() {
        nextUserIdRequiringDeviceVerification(
                activeDeviceVerificationStatesLogic = activeDeviceVerificationStatesLogic,
                routeNavigation = routeNavigation,
            )
            .collect(::updateNavigation)
    }

    private fun updateNavigation(userId: UserId) {
        routeNavigation.updateNavigation { push(VerificationRoute(userId)) }
    }
}

private fun nextUserIdRequiringDeviceVerification(
    activeDeviceVerificationStatesLogic: ActiveDeviceVerificationStatesLogic,
    routeNavigation: RouteNavigation,
): Flow<UserId> {
    return combine(
            activeDeviceVerificationStatesLogic.activeDeviceVerifications(),
            canShowDeviceVerification(routeNavigation),
            ::nextUserId,
        )
        .filterNotNull()
}

private fun nextUserId(
    activeDeviceVerificationStates: Map<UserId, ActiveVerificationState?>,
    canShowDeviceVerification: Boolean,
): UserId? {
    return if (!canShowDeviceVerification) null
    else activeDeviceVerificationStates.filter { shouldShowDeviceVerification(it.value) }.keys.firstOrNull()
}

private fun shouldShowDeviceVerification(activeVerificationState: ActiveVerificationState?): Boolean {
    return when (activeVerificationState) {
        is ActiveVerificationState.OwnRequest -> true
        is ActiveVerificationState.TheirRequest -> true
        is ActiveVerificationState.AcceptedByOtherDevice -> false
        is ActiveVerificationState.Cancel -> false
        is ActiveVerificationState.Done -> false
        is ActiveVerificationState.Ready -> false
        is ActiveVerificationState.Start -> false
        is ActiveVerificationState.Undefined -> false
        is ActiveVerificationState.WaitForDone -> false
        null -> false
    }
}

private fun canShowDeviceVerification(routeNavigation: RouteNavigation): Flow<Boolean> {
    return routeNavigation.routes.map { it.filterIsInstance<VerificationRoute>().isEmpty() }
}
