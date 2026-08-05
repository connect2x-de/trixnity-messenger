package de.connect2x.trixnity.messenger.internal.compose.view.entries

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.compose.view.settings.AccountSetupWizard
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.rememberComponent
import de.connect2x.trixnity.messenger.internal.compose.view.navigation.NavigationEntry
import de.connect2x.trixnity.messenger.internal.routes.AccountSetupRoute
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountSetupRouter
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountSetupViewModel

internal class AccountSetupEntry : NavigationEntry<AccountSetupRoute> {

    @Composable
    override fun Content(route: AccountSetupRoute) {
        AccountSetupWizard(
            showAccountBootstrapWrapper =
                AccountSetupRouter.Wrapper.ShowAccountSetup(viewModel = rememberComponent<AccountSetupViewModel>())
        )
    }
}
