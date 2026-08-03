package de.connect2x.trixnity.messenger.internal.adapters

import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.ViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.navigation.navigationCallback
import de.connect2x.trixnity.messenger.internal.routes.roomlist.NotificationSettingsRoute
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.settings.NotificationSettingsAllAccountsViewModel
import de.connect2x.trixnity.messenger.viewmodel.settings.NotificationSettingsAllAccountsViewModelFactory
import org.koin.core.parameter.ParametersHolder

internal class NotificationSettingsViewModelFactoryAdapter(
    private val factory: NotificationSettingsAllAccountsViewModelFactory,
    private val routeNavigation: RouteNavigation,
) : ViewModelFactoryAdapter<NotificationSettingsAllAccountsViewModel> {
    override fun create(parameters: ParametersHolder): NotificationSettingsAllAccountsViewModel {
        val route = parameters.get<NotificationSettingsRoute>()
        val viewModelContext = parameters.get<ViewModelContext>().childContext("NotificationSettings")

        return factory.create(
            viewModelContext = viewModelContext,
            onBack = routeNavigation.navigationCallback { pop(route) },
        )
    }
}
