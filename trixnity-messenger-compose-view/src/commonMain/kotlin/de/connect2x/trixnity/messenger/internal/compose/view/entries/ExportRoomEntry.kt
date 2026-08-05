package de.connect2x.trixnity.messenger.internal.compose.view.entries

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.compose.view.room.settings.ExportRoomContainer
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedSurface
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.rememberComponent
import de.connect2x.trixnity.messenger.internal.compose.view.navigation.NavigationEntry
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.threepane.ThreePaneScene
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.twopane.TwoPaneScene
import de.connect2x.trixnity.messenger.internal.routes.extras.ExportRoomRoute
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExportRoomViewModel

internal class ExportRoomEntry : NavigationEntry<ExportRoomRoute> {

    override val metadata: Map<String, Any> = TwoPaneScene.right() + ThreePaneScene.right()

    @Composable
    override fun Content(route: ExportRoomRoute) {
        ThemedSurface(style = MaterialTheme.components.details) {
            ExportRoomContainer(exportRoomViewModel = rememberComponent<ExportRoomViewModel>())
        }
    }
}
