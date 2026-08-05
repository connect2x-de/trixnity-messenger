package de.connect2x.trixnity.messenger.internal.compose.view.entries

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.compose.view.connecting.RemoveMatrixAccount
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.rememberComponent
import de.connect2x.trixnity.messenger.internal.compose.view.navigation.NavigationEntry
import de.connect2x.trixnity.messenger.internal.routes.root.RemoveMatrixAccountRoute
import de.connect2x.trixnity.messenger.viewmodel.connecting.RemoveMatrixAccountViewModel

internal class RemoveMatrixAccountEntry : NavigationEntry<RemoveMatrixAccountRoute> {

    @Composable
    override fun Content(route: RemoveMatrixAccountRoute) {
        RemoveMatrixAccount(removeMatrixAccountViewModel = rememberComponent<RemoveMatrixAccountViewModel>())
    }
}
