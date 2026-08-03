package de.connect2x.trixnity.messenger.internal.adapters

import de.connect2x.trixnity.messenger.internal.navigation.ResultEventBus
import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.ViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.navigation.clear
import de.connect2x.trixnity.messenger.internal.navigation.navigationCallback
import de.connect2x.trixnity.messenger.internal.navigation.replace
import de.connect2x.trixnity.messenger.internal.navigation.sendResult
import de.connect2x.trixnity.messenger.internal.routes.selfverification.CrossSigningBootstrapRoute
import de.connect2x.trixnity.messenger.internal.routes.selfverification.SelfVerificationRoute
import de.connect2x.trixnity.messenger.internal.routes.selfverification.SelfVerificationRouteMarker
import de.connect2x.trixnity.messenger.internal.util.AnySelfVerificationViewModel
import de.connect2x.trixnity.messenger.internal.util.AnySelfVerificationViewModelFactory
import de.connect2x.trixnity.messenger.internal.verification.ChangeVerificationCompleteStatus
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import org.koin.core.parameter.ParametersHolder

internal class SelfVerificationViewModelFactoryAdapter(
    private val factory: AnySelfVerificationViewModelFactory,
    private val routeNavigation: RouteNavigation,
    private val resultEventBus: ResultEventBus,
) : ViewModelFactoryAdapter<AnySelfVerificationViewModel> {
    override fun create(parameters: ParametersHolder): AnySelfVerificationViewModel {
        val route = parameters.get<SelfVerificationRoute>()
        val viewModelContext = parameters.get<ViewModelContext>().childContext("SelfVerification", route.userId)

        return factory.create(
            viewModelContext = viewModelContext,
            onCloseSelfVerification =
                routeNavigation.navigationCallback { completedVerification ->
                    resultEventBus.sendResult(ChangeVerificationCompleteStatus(route.userId, completedVerification))
                    clear<SelfVerificationRouteMarker>()
                },
            onResetRecovery =
                routeNavigation.navigationCallback {
                    replace<SelfVerificationRouteMarker>(CrossSigningBootstrapRoute(route.userId))
                },
        )
    }
}
