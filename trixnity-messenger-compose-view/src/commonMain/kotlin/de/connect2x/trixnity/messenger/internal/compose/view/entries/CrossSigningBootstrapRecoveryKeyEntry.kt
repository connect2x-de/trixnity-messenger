package de.connect2x.trixnity.messenger.internal.compose.view.entries

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.compose.view.verification.v2.CrossSigningBootstrapRecoveryKeyWizardPanel
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.ComponentStoreNavEntryDecorator.Companion.toMetadata
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.rememberComponentForKey
import de.connect2x.trixnity.messenger.internal.compose.view.navigation.NavigationEntry
import de.connect2x.trixnity.messenger.internal.routes.selfverification.CrossSigningBootstrapRecoveryKeyRoute
import de.connect2x.trixnity.messenger.viewmodel.verification.CrossSigningBootstrapViewModel

internal class CrossSigningBootstrapRecoveryKeyEntry : NavigationEntry<CrossSigningBootstrapRecoveryKeyRoute> {

    override fun metadata(route: CrossSigningBootstrapRecoveryKeyRoute): Map<String, Any> {
        return CrossSigningBootstrapEntry.sharingContext(route.userId).toMetadata()
    }

    @Composable
    override fun Content(route: CrossSigningBootstrapRecoveryKeyRoute) {
        CrossSigningBootstrapRecoveryKeyWizardPanel(
            rememberComponentForKey<CrossSigningBootstrapViewModel>(
                CrossSigningBootstrapEntry.Companion.CrossSigningBootstrapComponentStoreKey
            )
        )
    }
}
