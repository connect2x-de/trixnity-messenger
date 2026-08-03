package de.connect2x.trixnity.messenger.internal.adapters

import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.ViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.navigation.navigationCallback
import de.connect2x.trixnity.messenger.internal.routes.roomlist.AppInfoRoute
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.settings.AppInfoViewModel
import de.connect2x.trixnity.messenger.viewmodel.settings.AppInfoViewModelFactory
import org.koin.core.parameter.ParametersHolder

internal class AppInfoViewModelFactoryAdapter(
    private val factory: AppInfoViewModelFactory,
    private val routeNavigation: RouteNavigation,
) : ViewModelFactoryAdapter<AppInfoViewModel> {
    override fun create(parameters: ParametersHolder): AppInfoViewModel {
        val route = parameters.get<AppInfoRoute>()
        val viewModelContext = parameters.get<ViewModelContext>().childContext("AppInfo")

        return factory.create(
            viewModelContext = viewModelContext,
            onCloseAppInfo = routeNavigation.navigationCallback { pop(route) },
        )
    }
}
