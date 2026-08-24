package de.connect2x.trixnity.messenger.internal.adapters

import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.ViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.navigation.navigationCallback
import de.connect2x.trixnity.messenger.internal.routes.SyncRoute
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.initialsync.SyncViewModel
import de.connect2x.trixnity.messenger.viewmodel.initialsync.SyncViewModelFactory
import org.koin.core.parameter.ParametersHolder

internal class SyncViewModelFactoryAdapter(
    private val factory: SyncViewModelFactory,
    private val routeNavigation: RouteNavigation,
) : ViewModelFactoryAdapter<SyncViewModel> {
    override fun create(parameters: ParametersHolder): SyncViewModel {
        val route = parameters.get<SyncRoute>()
        val viewModelContext = parameters.get<ViewModelContext>().childContext("Sync")

        return factory.create(
            viewModelContext = viewModelContext,
            onSyncDone = routeNavigation.navigationCallback { pop(route) },
        )
    }
}
