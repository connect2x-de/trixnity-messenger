package de.connect2x.trixnity.messenger.internal.adapters

import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.ViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.navigation.navigationCallback
import de.connect2x.trixnity.messenger.internal.routes.ShareDataRoute
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.sharing.ShareDataViewModel
import de.connect2x.trixnity.messenger.viewmodel.sharing.ShareDataViewModelFactory
import org.koin.core.parameter.ParametersHolder

internal class ShareDataViewModelFactoryAdapter(
    private val factory: ShareDataViewModelFactory,
    private val routeNavigation: RouteNavigation,
) : ViewModelFactoryAdapter<ShareDataViewModel> {
    override fun create(parameters: ParametersHolder): ShareDataViewModel {
        val route = parameters.get<ShareDataRoute>()
        val viewModelContext = parameters.get<ViewModelContext>().childContext("ShareData")

        return factory.create(
            viewModelContext = viewModelContext,
            sharedData = route.data,
            onClose = routeNavigation.navigationCallback { pop(route) },
        )
    }
}
