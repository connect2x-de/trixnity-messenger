package de.connect2x.trixnity.messenger.internal.compose.view.entries.verification

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.compose.view.verification.v2.VerificationWaitForOtherWizardPanel
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.ComponentStoreNavEntryDecorator.Companion.toMetadata
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.rememberComponentForKey
import de.connect2x.trixnity.messenger.internal.compose.view.navigation.NavigationEntry
import de.connect2x.trixnity.messenger.internal.routes.verification.VerificationWaitRoute
import de.connect2x.trixnity.messenger.viewmodel.verification.v2.VerificationViewModel

internal class VerificationWaitForOtherClientEntry : NavigationEntry<VerificationWaitRoute> {
    override fun metadata(route: VerificationWaitRoute): Map<String, Any> {
        return VerificationEntry.sharingContext(route.userId).toMetadata()
    }

    @Composable
    override fun Content(route: VerificationWaitRoute) {
        VerificationWaitForOtherWizardPanel(
            rememberComponentForKey<VerificationViewModel>(VerificationEntry.Companion.VerificationComponentStoreKey)
        )
    }
}
