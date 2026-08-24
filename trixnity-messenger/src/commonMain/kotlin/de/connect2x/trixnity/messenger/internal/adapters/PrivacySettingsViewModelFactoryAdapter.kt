package de.connect2x.trixnity.messenger.internal.adapters

import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.ViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.navigation.navigationCallback
import de.connect2x.trixnity.messenger.internal.routes.roomlist.BlockedContactsSettingsRoute
import de.connect2x.trixnity.messenger.internal.routes.roomlist.PrivacySettingsRoute
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.settings.PrivacySettingsAllAccountsViewModel
import de.connect2x.trixnity.messenger.viewmodel.settings.PrivacySettingsAllAccountsViewModelFactory
import org.koin.core.parameter.ParametersHolder

internal class PrivacySettingsViewModelFactoryAdapter(
    private val factory: PrivacySettingsAllAccountsViewModelFactory,
    private val routeNavigation: RouteNavigation,
) : ViewModelFactoryAdapter<PrivacySettingsAllAccountsViewModel> {
    override fun create(parameters: ParametersHolder): PrivacySettingsAllAccountsViewModel {
        val route = parameters.get<PrivacySettingsRoute>()
        val viewModelContext = parameters.get<ViewModelContext>().childContext("PrivacySettings")

        return factory.create(
            viewModelContext = viewModelContext,
            onShowBlockedContactsSettings =
                routeNavigation.navigationCallback { userId -> push(BlockedContactsSettingsRoute(userId)) },
            onClosePrivacySettings = routeNavigation.navigationCallback { pop(route) },
        )
    }
}
