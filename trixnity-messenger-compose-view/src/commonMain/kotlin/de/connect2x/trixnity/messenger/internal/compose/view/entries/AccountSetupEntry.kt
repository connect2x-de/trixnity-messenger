package de.connect2x.trixnity.messenger.internal.compose.view.entries

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.ComponentStoreContext
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.ComponentStoreKey
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.ComponentStoreNavEntryDecorator
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.rememberComponent
import de.connect2x.trixnity.messenger.internal.compose.view.navigation.NavigationEntry
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.NavigationVisibility
import de.connect2x.trixnity.messenger.internal.routes.AccountSetupRoute
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountSetupViewModel

internal class AccountSetupEntry : NavigationEntry<AccountSetupRoute> {
    override val metadata: Map<String, Any> = NavigationVisibility.Hidden.metadata()

    @Composable
    override fun Content(route: AccountSetupRoute) {
        val viewModel = rememberComponent<AccountSetupViewModel>()
        LaunchedEffect(Unit) { viewModel.startAccountSetup() }
    }

    companion object {
        data object AccountSetupComponentStoreKey : ComponentStoreKey

        fun sharingContext(userId: UserId): ComponentStoreContext {
            return ComponentStoreNavEntryDecorator.context(AccountSetupComponentStoreKey, AccountSetupRoute(userId))
        }
    }
}
