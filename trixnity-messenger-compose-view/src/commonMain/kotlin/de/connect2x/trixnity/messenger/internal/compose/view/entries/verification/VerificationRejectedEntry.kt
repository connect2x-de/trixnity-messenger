package de.connect2x.trixnity.messenger.internal.compose.view.entries.verification

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.compose.view.verification.v2.VerificationRejectedWizardPanel
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.ComponentStoreNavEntryDecorator.Companion.toMetadata
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.rememberComponentForKey
import de.connect2x.trixnity.messenger.internal.compose.view.navigation.NavigationEntry
import de.connect2x.trixnity.messenger.internal.routes.verification.VerificationRejectedRoute
import de.connect2x.trixnity.messenger.viewmodel.verification.v2.VerificationViewModel

internal class VerificationRejectedEntry : NavigationEntry<VerificationRejectedRoute> {

    override fun metadata(route: VerificationRejectedRoute): Map<String, Any> {
        return VerificationEntry.sharingContext(route.userId).toMetadata()
    }

    @Composable
    override fun Content(route: VerificationRejectedRoute) {
        VerificationRejectedWizardPanel(
            rememberComponentForKey<VerificationViewModel>(VerificationEntry.Companion.VerificationComponentStoreKey)
        )
    }
}
