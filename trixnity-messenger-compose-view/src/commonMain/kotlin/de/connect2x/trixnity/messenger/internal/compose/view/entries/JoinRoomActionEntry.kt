package de.connect2x.trixnity.messenger.internal.compose.view.entries

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.compose.view.room.JoinRoomAction
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.rememberComponent
import de.connect2x.trixnity.messenger.internal.compose.view.navigation.NavigationEntry
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.twopane.TwoPaneScene
import de.connect2x.trixnity.messenger.internal.routes.JoinRoomActionRoute
import de.connect2x.trixnity.messenger.viewmodel.room.JoinRoomActionViewModel

internal class JoinRoomActionEntry : NavigationEntry<JoinRoomActionRoute> {

    override val metadata: Map<String, Any> = TwoPaneScene.right()

    @Composable
    override fun Content(route: JoinRoomActionRoute) {
        JoinRoomAction(viewModel = rememberComponent<JoinRoomActionViewModel>())
    }
}
