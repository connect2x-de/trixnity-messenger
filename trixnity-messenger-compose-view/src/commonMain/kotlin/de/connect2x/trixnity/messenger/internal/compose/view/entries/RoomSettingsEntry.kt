package de.connect2x.trixnity.messenger.internal.compose.view.entries

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.compose.view.room.settings.RoomSettingsContainer
import de.connect2x.trixnity.messenger.compose.view.root.IsSinglePane
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedSurface
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.rememberComponent
import de.connect2x.trixnity.messenger.internal.compose.view.navigation.NavigationEntry
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.threepane.ThreePaneScene
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.twopane.TwoPaneScene
import de.connect2x.trixnity.messenger.internal.routes.extras.RoomSettingsRoute
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsViewModel

internal class RoomSettingsEntry : NavigationEntry<RoomSettingsRoute> {

    override val metadata: Map<String, Any> = TwoPaneScene.right() + ThreePaneScene.right()

    @Composable
    override fun Content(route: RoomSettingsRoute) {
        ThemedSurface(style = MaterialTheme.components.details) {
            RoomSettingsContainer(
                roomSettingsViewModel = rememberComponent<RoomSettingsViewModel>(),
                isSinglePane = IsSinglePane.current,
            )
        }
    }
}
