package de.connect2x.trixnity.messenger.internal.compose.view.entries

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.rememberComponent
import de.connect2x.trixnity.messenger.internal.compose.view.navigation.NavigationEntry
import de.connect2x.trixnity.messenger.internal.routes.selfverification.SelfVerificationRoute
import de.connect2x.trixnity.messenger.internal.util.AnySelfVerificationViewModel

internal class SelfVerificationEntry : NavigationEntry<SelfVerificationRoute> {

    @Composable
    override fun Content(route: SelfVerificationRoute) {
        AnySelfVerificationWizard(selfVerificationViewModel = rememberComponent<AnySelfVerificationViewModel>())
    }
}

@Composable
private fun AnySelfVerificationWizard(selfVerificationViewModel: AnySelfVerificationViewModel) {
    when (selfVerificationViewModel) {
        is AnySelfVerificationViewModel.V1 ->
            @Suppress("DEPRECATION")
            de.connect2x.trixnity.messenger.compose.view.verification.SelfVerificationWizard(
                selfVerificationViewModel = selfVerificationViewModel.value
            )
        is AnySelfVerificationViewModel.V2 ->
            de.connect2x.trixnity.messenger.compose.view.verification.v2.SelfVerificationWizard(
                selfVerificationViewModel = selfVerificationViewModel.value
            )
    }
}
