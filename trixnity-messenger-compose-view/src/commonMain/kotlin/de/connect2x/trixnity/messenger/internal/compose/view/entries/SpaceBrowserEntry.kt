package de.connect2x.trixnity.messenger.internal.compose.view.entries

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.compose.view.space.SpaceBrowserView
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.rememberComponent
import de.connect2x.trixnity.messenger.internal.compose.view.navigation.NavigationEntry
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.NavigationVisibility
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.navigationrail.NavigationRailScene
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.threepane.ThreePaneScene
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.twopane.TwoPaneScene
import de.connect2x.trixnity.messenger.internal.routes.spaces.SpaceBrowserRoute
import de.connect2x.trixnity.messenger.viewmodel.spaces.SpaceBrowserViewModel

internal class SpaceBrowserEntry : NavigationEntry<SpaceBrowserRoute> {
    override val metadata: Map<String, Any> =
        TwoPaneScene.left() +
            ThreePaneScene.left() +
            NavigationVisibility.Always.metadata() +
            NavigationRailScene.selection(SpaceBrowserRoute)

    @Composable
    override fun Content(route: SpaceBrowserRoute) {
        val viewModel = rememberComponent<SpaceBrowserViewModel>()
        SpaceBrowserView(viewModel.content, viewModel.account)
    }
}
