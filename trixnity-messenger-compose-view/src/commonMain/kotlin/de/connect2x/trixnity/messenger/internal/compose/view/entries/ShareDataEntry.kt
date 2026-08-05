package de.connect2x.trixnity.messenger.internal.compose.view.entries

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.compose.view.sharing.ShareData
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.rememberComponent
import de.connect2x.trixnity.messenger.internal.compose.view.navigation.NavigationEntry
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.overlay.OverlayScene
import de.connect2x.trixnity.messenger.internal.routes.ShareDataRoute
import de.connect2x.trixnity.messenger.viewmodel.sharing.ShareDataViewModel

internal class ShareDataEntry : NavigationEntry<ShareDataRoute> {

    override val metadata: Map<String, Any> = OverlayScene.overlay()

    @Composable
    override fun Content(route: ShareDataRoute) {
        ShareData(viewModel = rememberComponent<ShareDataViewModel>())
    }
}
