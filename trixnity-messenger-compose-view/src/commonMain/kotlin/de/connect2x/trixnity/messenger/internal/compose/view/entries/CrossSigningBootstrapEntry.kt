package de.connect2x.trixnity.messenger.internal.compose.view.entries

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.compose.view.verification.CrossSigningBootstrapWizard
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.rememberComponent
import de.connect2x.trixnity.messenger.internal.compose.view.navigation.NavigationEntry
import de.connect2x.trixnity.messenger.internal.routes.selfverification.CrossSigningBootstrapRoute
import de.connect2x.trixnity.messenger.viewmodel.verification.CrossSigningBootstrapViewModel

internal class CrossSigningBootstrapEntry : NavigationEntry<CrossSigningBootstrapRoute> {

    @Composable
    override fun Content(route: CrossSigningBootstrapRoute) {
        CrossSigningBootstrapWizard(
            crossSigningBootstrapViewModel = rememberComponent<CrossSigningBootstrapViewModel>()
        )
    }
}
