package de.connect2x.trixnity.messenger.internal.compose.view.entries

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.compose.view.connecting.RegisterMatrixAccountWizardPanel
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.rememberComponent
import de.connect2x.trixnity.messenger.internal.compose.view.navigation.NavigationEntry
import de.connect2x.trixnity.messenger.internal.routes.root.RegisterMatrixAccountRoute
import de.connect2x.trixnity.messenger.viewmodel.connecting.RegisterMatrixAccountViewModel

internal class RegisterMatrixAccountEntry : NavigationEntry<RegisterMatrixAccountRoute> {

    @Composable
    override fun Content(route: RegisterMatrixAccountRoute) {
        RegisterMatrixAccountWizardPanel(viewModel = rememberComponent<RegisterMatrixAccountViewModel>())
    }
}
