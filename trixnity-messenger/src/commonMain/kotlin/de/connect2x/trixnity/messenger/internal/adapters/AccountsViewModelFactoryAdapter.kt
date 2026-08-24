package de.connect2x.trixnity.messenger.internal.adapters

import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.ViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.navigation.navigationCallback
import de.connect2x.trixnity.messenger.internal.navigation.replace
import de.connect2x.trixnity.messenger.internal.routes.AccountSetupRoute
import de.connect2x.trixnity.messenger.internal.routes.AvatarCutterRoute
import de.connect2x.trixnity.messenger.internal.routes.roomlist.AccountsRoute
import de.connect2x.trixnity.messenger.internal.routes.root.AddMatrixAccountRoute
import de.connect2x.trixnity.messenger.internal.routes.root.RemoveMatrixAccountRoute
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountsViewModel
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountsViewModelFactory
import org.koin.core.parameter.ParametersHolder

internal class AccountsViewModelFactoryAdapter(
    private val factory: AccountsViewModelFactory,
    private val routeNavigation: RouteNavigation,
) : ViewModelFactoryAdapter<AccountsViewModel> {
    override fun create(parameters: ParametersHolder): AccountsViewModel {
        val route = parameters.get<AccountsRoute>()
        val viewModelContext = parameters.get<ViewModelContext>().childContext("Accounts")

        return factory.create(
            viewModelContext = viewModelContext,
            onCloseAccounts = routeNavigation.navigationCallback { pop(route) },
            onOpenAvatarCutter =
                routeNavigation.navigationCallback { userId, fileDescriptor ->
                    replace(AvatarCutterRoute(userId, null, fileDescriptor))
                },
            onShowAccountSetup = routeNavigation.navigationCallback { userId -> replace(AccountSetupRoute(userId)) },
            onRemoveAccount = routeNavigation.navigationCallback { userId -> push(RemoveMatrixAccountRoute(userId)) },
            onCreateNewAccount = routeNavigation.navigationCallback { items = listOf(AddMatrixAccountRoute) },
        )
    }
}
