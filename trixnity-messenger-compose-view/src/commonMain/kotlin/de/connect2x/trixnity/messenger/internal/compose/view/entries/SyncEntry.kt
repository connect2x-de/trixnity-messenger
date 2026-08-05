package de.connect2x.trixnity.messenger.internal.compose.view.entries

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.compose.view.root.SyncOverlay
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.rememberComponent
import de.connect2x.trixnity.messenger.internal.compose.view.navigation.NavigationEntry
import de.connect2x.trixnity.messenger.internal.routes.SyncRoute
import de.connect2x.trixnity.messenger.viewmodel.initialsync.SyncViewModel

internal class SyncEntry : NavigationEntry<SyncRoute> {

    @Composable
    override fun Content(route: SyncRoute) {
        SyncOverlay(syncViewModel = rememberComponent<SyncViewModel>())
    }
}
