package de.connect2x.trixnity.messenger.internal.adapters

import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.ViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.navigation.navigationCallback
import de.connect2x.trixnity.messenger.internal.navigation.replace
import de.connect2x.trixnity.messenger.internal.routes.selfverification.RedoSelfVerificationRoute
import de.connect2x.trixnity.messenger.internal.routes.selfverification.SelfVerificationRoute
import de.connect2x.trixnity.messenger.internal.routes.selfverification.SelfVerificationRouteMarker
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.verification.RedoSelfVerificationViewModel
import de.connect2x.trixnity.messenger.viewmodel.verification.RedoSelfVerificationViewModelFactory
import org.koin.core.parameter.ParametersHolder

internal class RedoSelfVerificationViewModelFactoryAdapter(
    private val factory: RedoSelfVerificationViewModelFactory,
    private val routeNavigation: RouteNavigation,
) : ViewModelFactoryAdapter<RedoSelfVerificationViewModel> {
    override fun create(parameters: ParametersHolder): RedoSelfVerificationViewModel {
        val route = parameters.get<RedoSelfVerificationRoute>()
        val viewModelContext = parameters.get<ViewModelContext>().childContext("RedoSelfVerification", route.userId)

        return factory.create(
            viewModelContext = viewModelContext,
            onStartSelfVerification =
                routeNavigation.navigationCallback {
                    replace<SelfVerificationRouteMarker>(SelfVerificationRoute(route.userId))
                },
            onClose = routeNavigation.navigationCallback { pop(route) },
        )
    }
}
