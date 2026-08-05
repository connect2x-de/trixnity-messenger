package de.connect2x.trixnity.messenger.internal.compose.view.entries

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.compose.view.connecting.ConnectingWizard
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.rememberComponent
import de.connect2x.trixnity.messenger.internal.compose.view.navigation.NavigationEntry
import de.connect2x.trixnity.messenger.internal.routes.root.AddMatrixAccountRoute
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountViewModel

internal class AddMatrixAccountEntry : NavigationEntry<AddMatrixAccountRoute> {

    @Composable
    override fun Content(route: AddMatrixAccountRoute) {
        ConnectingWizard(viewModel = rememberComponent<AddMatrixAccountViewModel>())
    }
}
