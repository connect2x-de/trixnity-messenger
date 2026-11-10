package de.connect2x.trixnity.messenger.internal.compose.view.entries

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.compose.view.settings.AccountSetupSettingsWizardPanel
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.ComponentStoreNavEntryDecorator.Companion.toMetadata
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.rememberComponentForKey
import de.connect2x.trixnity.messenger.internal.compose.view.navigation.NavigationEntry
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.NavigationVisibility
import de.connect2x.trixnity.messenger.internal.routes.AccountSetupSettingsRoute
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountSetupViewModel

internal class AccountSetupSettingsEntry : NavigationEntry<AccountSetupSettingsRoute> {

    override fun metadata(route: AccountSetupSettingsRoute): Map<String, Any> {
        return AccountSetupEntry.sharingContext(route.userId).toMetadata() + NavigationVisibility.Hidden.metadata()
    }

    @Composable
    override fun Content(route: AccountSetupSettingsRoute) {
        AccountSetupSettingsWizardPanel(
            rememberComponentForKey<AccountSetupViewModel>(AccountSetupEntry.Companion.AccountSetupComponentStoreKey)
        )
    }
}
