package de.connect2x.trixnity.messenger.internal.adapters

import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.ViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.navigation.navigationCallback
import de.connect2x.trixnity.messenger.internal.routes.roomlist.DeviceSettingsRoute
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.settings.DeviceSettingsAllAccountsViewModel
import de.connect2x.trixnity.messenger.viewmodel.settings.DeviceSettingsAllAccountsViewModelFactory
import org.koin.core.parameter.ParametersHolder

internal class DeviceSettingsViewModelFactoryAdapter(
    private val factory: DeviceSettingsAllAccountsViewModelFactory,
    private val routeNavigation: RouteNavigation,
) : ViewModelFactoryAdapter<DeviceSettingsAllAccountsViewModel> {
    override fun create(parameters: ParametersHolder): DeviceSettingsAllAccountsViewModel {
        val route = parameters.get<DeviceSettingsRoute>()
        val viewModelContext = parameters.get<ViewModelContext>().childContext("DeviceSettings")

        return factory.create(
            viewModelContext = viewModelContext,
            onCloseDeviceSettings = routeNavigation.navigationCallback { pop(route) },
        )
    }
}
