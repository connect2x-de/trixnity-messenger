package de.connect2x.trixnity.messenger.internal.adapters

import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.ViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.navigation.navigationCallback
import de.connect2x.trixnity.messenger.internal.navigation.openSpaceItem
import de.connect2x.trixnity.messenger.internal.routes.roomlist.AccountsRoute
import de.connect2x.trixnity.messenger.internal.routes.roomlist.AppInfoRoute
import de.connect2x.trixnity.messenger.internal.routes.roomlist.UserSettingsRoute
import de.connect2x.trixnity.messenger.internal.routes.spaces.SpaceBrowserRoute
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.spaces.SpaceBrowserViewModel
import de.connect2x.trixnity.messenger.viewmodel.spaces.SpaceBrowserViewModelFactory
import org.koin.core.parameter.ParametersHolder

internal class SpaceBrowserViewModelFactoryAdapter(
    private val factory: SpaceBrowserViewModelFactory,
    private val navigation: RouteNavigation,
) : ViewModelFactoryAdapter<SpaceBrowserViewModel> {
    override fun create(parameters: ParametersHolder): SpaceBrowserViewModel {
        val route = parameters.get<SpaceBrowserRoute>()
        val context = parameters.get<ViewModelContext>().childContext("SpaceBrowser")
        return factory.create(
            viewModelContext = context,
            onSelect = navigation.navigationCallback { item -> openSpaceItem(item, showExplore = false) },
            onBack = navigation.navigationCallback { pop(route) },
            onAccountSelected = {},
            onUserSettingsSelected = navigation.navigationCallback { push(UserSettingsRoute) },
            onShowAppInfo = navigation.navigationCallback { push(AppInfoRoute) },
            onShowAccounts = navigation.navigationCallback { push(AccountsRoute) },
        )
    }
}
