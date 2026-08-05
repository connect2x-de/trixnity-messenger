package de.connect2x.trixnity.messenger.internal.compose.view.entries

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.compose.view.connecting.MatrixClientInitialization
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.rememberComponent
import de.connect2x.trixnity.messenger.internal.compose.view.navigation.NavigationEntry
import de.connect2x.trixnity.messenger.internal.routes.root.MatrixClientInitializationRoute
import de.connect2x.trixnity.messenger.viewmodel.connecting.MatrixClientInitializationViewModel

internal class MatrixClientInitializationEntry : NavigationEntry<MatrixClientInitializationRoute> {

    @Composable
    override fun Content(route: MatrixClientInitializationRoute) {
        MatrixClientInitialization(
            matrixClientInitializationViewModel = rememberComponent<MatrixClientInitializationViewModel>()
        )
    }
}
