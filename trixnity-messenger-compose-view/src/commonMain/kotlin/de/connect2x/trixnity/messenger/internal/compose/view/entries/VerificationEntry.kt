package de.connect2x.trixnity.messenger.internal.compose.view.entries

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.compose.view.verification.DeviceVerificationWizard
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.rememberComponent
import de.connect2x.trixnity.messenger.internal.compose.view.navigation.NavigationEntry
import de.connect2x.trixnity.messenger.internal.routes.VerificationRoute
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModel

internal class VerificationEntry : NavigationEntry<VerificationRoute> {

    @Composable
    override fun Content(route: VerificationRoute) {
        DeviceVerificationWizard(verificationViewModel = rememberComponent<VerificationViewModel>())
    }
}
