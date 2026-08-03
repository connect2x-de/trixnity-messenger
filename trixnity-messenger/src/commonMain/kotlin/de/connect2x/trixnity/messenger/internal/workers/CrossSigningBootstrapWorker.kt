package de.connect2x.trixnity.messenger.internal.workers

import de.connect2x.trixnity.client.verification.VerificationService.SelfVerificationMethods
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.Worker
import de.connect2x.trixnity.messenger.internal.logic.MatrixClientSelfVerificationMethodsLogic
import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.routes.AccountSetupRoute
import de.connect2x.trixnity.messenger.internal.routes.selfverification.CrossSigningBootstrapRoute
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

internal interface CrossSigningBootstrapWorker : Worker

internal fun CrossSigningBootstrapWorker(
    selfVerificationMethods: MatrixClientSelfVerificationMethodsLogic,
    routeNavigation: RouteNavigation,
): CrossSigningBootstrapWorker {
    return CrossSigningBootstrapWorkerImpl(
        selfVerificationMethods = selfVerificationMethods,
        routeNavigation = routeNavigation,
    )
}

private class CrossSigningBootstrapWorkerImpl(
    private val selfVerificationMethods: MatrixClientSelfVerificationMethodsLogic,
    private val routeNavigation: RouteNavigation,
) : CrossSigningBootstrapWorker {
    override suspend fun doWork() {
        showCrossSigningBootstrapFor(
                selfVerificationMethods = selfVerificationMethods,
                routeNavigation = routeNavigation,
            )
            .collect(::pushCrossSigningBootstrapRoute)
    }

    private fun pushCrossSigningBootstrapRoute(userId: UserId) {
        routeNavigation.updateNavigation { push(CrossSigningBootstrapRoute(userId)) }
    }
}

private fun showCrossSigningBootstrapFor(
    selfVerificationMethods: MatrixClientSelfVerificationMethodsLogic,
    routeNavigation: RouteNavigation,
): Flow<UserId> {
    return combine(accountSetupUserId(routeNavigation), crossSigningNotEnabledUserIds(selfVerificationMethods)) {
            userId,
            userIds ->
            userId?.takeIf { userIds.contains(it) }
        }
        .filterNotNull()
}

private fun crossSigningNotEnabledUserIds(
    selfVerificationMethods: MatrixClientSelfVerificationMethodsLogic
): Flow<Set<UserId>> {
    return selfVerificationMethods.selfVerificationMethods().map {
        it.filterValues { it is SelfVerificationMethods.NoCrossSigningEnabled }.keys
    }
}

private fun accountSetupUserId(routeNavigation: RouteNavigation): Flow<UserId?> {
    return routeNavigation.routes.map { (it.lastOrNull() as? AccountSetupRoute)?.userId }
}
