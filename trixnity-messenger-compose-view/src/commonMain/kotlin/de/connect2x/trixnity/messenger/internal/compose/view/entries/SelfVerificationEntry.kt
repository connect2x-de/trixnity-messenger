package de.connect2x.trixnity.messenger.internal.compose.view.entries

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.compose.view.verification.v2.SelfVerificationWizardPanel
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.rememberComponent
import de.connect2x.trixnity.messenger.internal.compose.view.navigation.NavigationEntry
import de.connect2x.trixnity.messenger.internal.routes.selfverification.SelfVerificationRoute
import de.connect2x.trixnity.messenger.viewmodel.verification.v2.SelfVerificationViewModel

internal class SelfVerificationEntry : NavigationEntry<SelfVerificationRoute> {

    @Composable
    override fun Content(route: SelfVerificationRoute) {
        SelfVerificationWizardPanel(rememberComponent<SelfVerificationViewModel>())
    }
}
