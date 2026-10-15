package de.connect2x.trixnity.messenger.internal.adapters

import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.ViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.navigation.navigationCallback
import de.connect2x.trixnity.messenger.internal.routes.roomlist.ExperimentalSettingsRoute
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.settings.ExperimentalSettingsViewModel
import de.connect2x.trixnity.messenger.viewmodel.settings.ExperimentalSettingsViewModelFactory
import org.koin.core.parameter.ParametersHolder

internal class ExperimentalSettingsViewModelFactoryAdapter(
    private val factory: ExperimentalSettingsViewModelFactory,
    private val routeNavigation: RouteNavigation,
) : ViewModelFactoryAdapter<ExperimentalSettingsViewModel> {
    override fun create(parameters: ParametersHolder): ExperimentalSettingsViewModel {
        val route = parameters.get<ExperimentalSettingsRoute>()
        val viewModelContext = parameters.get<ViewModelContext>().childContext("ExperimentalSettings")

        return factory.create(
            viewModelContext = viewModelContext,
            onCloseExperimentalSettings = routeNavigation.navigationCallback { pop(route) },
        )
    }
}
