package de.connect2x.trixnity.messenger.internal.compose.view.entries

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.compose.view.verification.v2.CrossSigningBootstrapExplanationWizardPanel
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.ComponentStoreNavEntryDecorator.Companion.toMetadata
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.rememberComponentForKey
import de.connect2x.trixnity.messenger.internal.compose.view.navigation.NavigationEntry
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.NavigationVisibility
import de.connect2x.trixnity.messenger.internal.routes.selfverification.CrossSigningBootstrapExplanationRoute
import de.connect2x.trixnity.messenger.viewmodel.verification.CrossSigningBootstrapViewModel

internal class CrossSigningBootstrapExplanationEntry : NavigationEntry<CrossSigningBootstrapExplanationRoute> {
    override fun metadata(route: CrossSigningBootstrapExplanationRoute): Map<String, Any> {
        return CrossSigningBootstrapEntry.sharingContext(route.userId).toMetadata() +
            NavigationVisibility.Hidden.metadata()
    }

    @Composable
    override fun Content(route: CrossSigningBootstrapExplanationRoute) {
        CrossSigningBootstrapExplanationWizardPanel(
            rememberComponentForKey<CrossSigningBootstrapViewModel>(
                CrossSigningBootstrapEntry.Companion.CrossSigningBootstrapComponentStoreKey
            )
        )
    }
}
