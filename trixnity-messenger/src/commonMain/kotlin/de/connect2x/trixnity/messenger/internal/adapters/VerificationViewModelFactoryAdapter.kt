package de.connect2x.trixnity.messenger.internal.adapters

import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.ViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.navigation.clear
import de.connect2x.trixnity.messenger.internal.navigation.navigationCallback
import de.connect2x.trixnity.messenger.internal.navigation.replace
import de.connect2x.trixnity.messenger.internal.routes.VerificationRoute
import de.connect2x.trixnity.messenger.internal.routes.selfverification.RedoSelfVerificationRoute
import de.connect2x.trixnity.messenger.internal.routes.selfverification.SelfVerificationRouteMarker
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModel
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModelFactory
import org.koin.core.parameter.ParametersHolder

internal class VerificationViewModelFactoryAdapter(
    private val factory: VerificationViewModelFactory,
    private val routeNavigation: RouteNavigation,
) : ViewModelFactoryAdapter<VerificationViewModel> {
    override fun create(parameters: ParametersHolder): VerificationViewModel {
        val route = parameters.get<VerificationRoute>()
        val viewModelContext = parameters.get<ViewModelContext>().childContext("Verification", route.userId)

        return factory.create(
            viewModelContext = viewModelContext,
            onCloseVerification = routeNavigation.navigationCallback { clear<VerificationRoute>() },
            onRedoSelfVerification =
                routeNavigation.navigationCallback {
                    clear<VerificationRoute>()
                    replace<SelfVerificationRouteMarker>(RedoSelfVerificationRoute(route.userId))
                },
            roomId = null,
            timelineEventId = null,
        )
    }
}
