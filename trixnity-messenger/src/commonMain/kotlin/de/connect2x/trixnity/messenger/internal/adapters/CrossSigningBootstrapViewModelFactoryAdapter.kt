package de.connect2x.trixnity.messenger.internal.adapters

import de.connect2x.trixnity.messenger.internal.navigation.ResultEventBus
import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.ViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.navigation.clear
import de.connect2x.trixnity.messenger.internal.navigation.navigationCallback
import de.connect2x.trixnity.messenger.internal.navigation.sendResult
import de.connect2x.trixnity.messenger.internal.routes.selfverification.CrossSigningBootstrapRoute
import de.connect2x.trixnity.messenger.internal.routes.selfverification.SelfVerificationRouteMarker
import de.connect2x.trixnity.messenger.internal.verification.ChangeVerificationCompleteStatus
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.verification.CrossSigningBootstrapViewModel
import de.connect2x.trixnity.messenger.viewmodel.verification.CrossSigningBootstrapViewModelFactory
import org.koin.core.parameter.ParametersHolder

internal class CrossSigningBootstrapViewModelFactoryAdapter(
    private val factory: CrossSigningBootstrapViewModelFactory,
    private val routeNavigation: RouteNavigation,
    private val resultEventBus: ResultEventBus,
) : ViewModelFactoryAdapter<CrossSigningBootstrapViewModel> {
    override fun create(parameters: ParametersHolder): CrossSigningBootstrapViewModel {
        val route = parameters.get<CrossSigningBootstrapRoute>()
        val viewModelContext = parameters.get<ViewModelContext>().childContext("CrossSigningBootstrap", route.userId)

        return factory.create(
            viewModelContext = viewModelContext,
            onClose =
                routeNavigation.navigationCallback { userId ->
                    resultEventBus.sendResult(ChangeVerificationCompleteStatus(userId, true))
                    clear<SelfVerificationRouteMarker>()
                },
        )
    }
}
