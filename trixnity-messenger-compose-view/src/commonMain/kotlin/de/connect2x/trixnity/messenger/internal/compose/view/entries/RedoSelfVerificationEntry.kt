package de.connect2x.trixnity.messenger.internal.compose.view.entries

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.compose.view.verification.v2.RedoSelfVerificationWizardPanel
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.rememberComponent
import de.connect2x.trixnity.messenger.internal.compose.view.navigation.NavigationEntry
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.NavigationVisibility
import de.connect2x.trixnity.messenger.internal.routes.selfverification.RedoSelfVerificationRoute
import de.connect2x.trixnity.messenger.viewmodel.verification.RedoSelfVerificationViewModel

internal class RedoSelfVerificationEntry : NavigationEntry<RedoSelfVerificationRoute> {
    override val metadata: Map<String, Any> = NavigationVisibility.Hidden.metadata()

    @Composable
    override fun Content(route: RedoSelfVerificationRoute) {
        RedoSelfVerificationWizardPanel(
            redoSelfVerificationViewModel = rememberComponent<RedoSelfVerificationViewModel>()
        )
    }
}
