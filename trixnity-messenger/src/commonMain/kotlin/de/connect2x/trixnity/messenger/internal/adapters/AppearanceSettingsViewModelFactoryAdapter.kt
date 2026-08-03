package de.connect2x.trixnity.messenger.internal.adapters

import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.ViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.navigation.navigationCallback
import de.connect2x.trixnity.messenger.internal.routes.roomlist.AppearanceSettingsRoute
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.settings.AppearanceSettingsViewModel
import de.connect2x.trixnity.messenger.viewmodel.settings.AppearanceSettingsViewModelFactory
import org.koin.core.parameter.ParametersHolder

internal class AppearanceSettingsViewModelFactoryAdapter(
    private val factory: AppearanceSettingsViewModelFactory,
    private val routeNavigation: RouteNavigation,
) : ViewModelFactoryAdapter<AppearanceSettingsViewModel> {
    override fun create(parameters: ParametersHolder): AppearanceSettingsViewModel {
        val route = parameters.get<AppearanceSettingsRoute>()
        val viewModelContext = parameters.get<ViewModelContext>().childContext("AppearanceSettings")

        return factory.create(
            viewModelContext = viewModelContext,
            onCloseAppearanceSettings = routeNavigation.navigationCallback { pop(route) },
        )
    }
}
