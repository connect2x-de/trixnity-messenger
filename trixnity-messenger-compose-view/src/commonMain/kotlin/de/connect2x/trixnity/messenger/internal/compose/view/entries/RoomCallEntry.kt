package de.connect2x.trixnity.messenger.internal.compose.view.entries

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.compose.view.room.call.RoomCall
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.rememberComponent
import de.connect2x.trixnity.messenger.internal.compose.view.navigation.NavigationEntry
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.call.CallControlsScene
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.threepane.ThreePaneScene
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.twopane.TwoPaneScene
import de.connect2x.trixnity.messenger.internal.routes.RoomCallRoute
import de.connect2x.trixnity.messenger.viewmodel.room.call.RoomCallViewModel

@TrixnityMessengerCallApi
internal class RoomCallEntry : NavigationEntry<RoomCallRoute> {
    override fun metadata(route: RoomCallRoute): Map<String, Any> {
        return TwoPaneScene.right() + ThreePaneScene.middle() + CallControlsScene.expandedCall(route.target)
    }

    @Composable
    override fun Content(route: RoomCallRoute) {
        RoomCall(rememberComponent<RoomCallViewModel>())
    }
}
