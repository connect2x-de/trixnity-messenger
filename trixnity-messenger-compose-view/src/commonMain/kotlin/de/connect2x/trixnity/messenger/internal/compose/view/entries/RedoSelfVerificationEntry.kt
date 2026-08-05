package de.connect2x.trixnity.messenger.internal.compose.view.entries

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.compose.view.verification.RedoSelfVerificationWizard
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.rememberComponent
import de.connect2x.trixnity.messenger.internal.compose.view.navigation.NavigationEntry
import de.connect2x.trixnity.messenger.internal.routes.selfverification.RedoSelfVerificationRoute
import de.connect2x.trixnity.messenger.viewmodel.verification.RedoSelfVerificationViewModel

internal class RedoSelfVerificationEntry : NavigationEntry<RedoSelfVerificationRoute> {

    @Composable
    override fun Content(route: RedoSelfVerificationRoute) {
        RedoSelfVerificationWizard(redoSelfVerificationViewModel = rememberComponent<RedoSelfVerificationViewModel>())
    }
}
