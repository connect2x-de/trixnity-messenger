package de.connect2x.trixnity.messenger.internal.compose.view.entries

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.compose.view.root.IsSinglePane
import de.connect2x.trixnity.messenger.compose.view.space.SpaceSettings
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedSurface
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.rememberComponent
import de.connect2x.trixnity.messenger.internal.compose.view.navigation.NavigationEntry
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.threepane.ThreePaneScene
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.twopane.TwoPaneScene
import de.connect2x.trixnity.messenger.internal.routes.spaces.SpaceSettingsRoute
import de.connect2x.trixnity.messenger.viewmodel.spaces.SpaceSettingsViewModel

internal class SpaceSettingsEntry : NavigationEntry<SpaceSettingsRoute> {

    override val metadata: Map<String, Any> = TwoPaneScene.right() + ThreePaneScene.right()

    @Composable
    override fun Content(route: SpaceSettingsRoute) {
        ThemedSurface(style = MaterialTheme.components.details) {
            SpaceSettings(viewModel = rememberComponent<SpaceSettingsViewModel>(), isSinglePane = IsSinglePane.current)
        }
    }
}
