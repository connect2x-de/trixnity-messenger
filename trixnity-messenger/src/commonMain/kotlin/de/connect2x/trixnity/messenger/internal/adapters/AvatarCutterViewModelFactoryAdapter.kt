package de.connect2x.trixnity.messenger.internal.adapters

import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.ViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.navigation.navigationCallback
import de.connect2x.trixnity.messenger.internal.routes.AvatarCutterRoute
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.settings.AvatarCutterViewModel
import de.connect2x.trixnity.messenger.viewmodel.settings.AvatarCutterViewModelFactory
import org.koin.core.parameter.ParametersHolder

internal class AvatarCutterViewModelFactoryAdapter(
    private val factory: AvatarCutterViewModelFactory,
    private val routeNavigation: RouteNavigation,
) : ViewModelFactoryAdapter<AvatarCutterViewModel> {
    override fun create(parameters: ParametersHolder): AvatarCutterViewModel {
        val route = parameters.get<AvatarCutterRoute>()
        val viewModelContext = parameters.get<ViewModelContext>().childContext("AvatarCutter", route.userId)

        return factory.create(
            viewModelContext = viewModelContext,
            file = route.fileDescriptor,
            roomId = route.roomId,
            onClose = routeNavigation.navigationCallback { pop(route) },
        )
    }
}
