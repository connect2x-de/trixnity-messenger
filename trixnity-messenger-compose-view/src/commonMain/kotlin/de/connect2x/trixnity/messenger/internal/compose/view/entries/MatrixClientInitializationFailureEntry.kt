package de.connect2x.trixnity.messenger.internal.compose.view.entries

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.compose.view.connecting.MatrixClientInitializationFailure
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.rememberComponent
import de.connect2x.trixnity.messenger.internal.compose.view.navigation.NavigationEntry
import de.connect2x.trixnity.messenger.internal.routes.root.MatrixClientInitializationFailureRoute
import de.connect2x.trixnity.messenger.viewmodel.connecting.MatrixClientInitializationFailureViewModel

internal class MatrixClientInitializationFailureEntry : NavigationEntry<MatrixClientInitializationFailureRoute> {

    @Composable
    override fun Content(route: MatrixClientInitializationFailureRoute) {
        MatrixClientInitializationFailure(
            matrixClientInitializationFailureViewModel = rememberComponent<MatrixClientInitializationFailureViewModel>()
        )
    }
}
