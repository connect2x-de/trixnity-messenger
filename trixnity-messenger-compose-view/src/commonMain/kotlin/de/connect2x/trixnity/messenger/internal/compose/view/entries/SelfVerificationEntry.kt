package de.connect2x.trixnity.messenger.internal.compose.view.entries

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.compose.view.verification.v2.SelfVerificationWizardPanel
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.rememberComponent
import de.connect2x.trixnity.messenger.internal.compose.view.navigation.NavigationEntry
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.NavigationVisibility
import de.connect2x.trixnity.messenger.internal.routes.selfverification.SelfVerificationRoute
import de.connect2x.trixnity.messenger.viewmodel.verification.v2.SelfVerificationViewModel

internal class SelfVerificationEntry : NavigationEntry<SelfVerificationRoute> {
    override val metadata: Map<String, Any> = NavigationVisibility.Hidden.metadata()

    @Composable
    override fun Content(route: SelfVerificationRoute) {
        SelfVerificationWizardPanel(rememberComponent<SelfVerificationViewModel>())
    }
}
