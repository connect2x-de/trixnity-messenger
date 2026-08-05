package de.connect2x.trixnity.messenger.internal.compose.view.entries

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import de.connect2x.trixnity.messenger.compose.view.room.timeline.RoomHeader
import de.connect2x.trixnity.messenger.compose.view.room.timeline.SendAttachment
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedSurface
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.ComponentStoreContext
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.ComponentStoreKey
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.ComponentStoreNavEntryDecorator
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.ComponentStoreNavEntryDecorator.Companion.plus
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.rememberComponent
import de.connect2x.trixnity.messenger.internal.compose.view.navigation.NavigationEntry
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.threepane.ThreePaneScene
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.twopane.TwoPaneScene
import de.connect2x.trixnity.messenger.internal.navigation.GetRouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.Route
import de.connect2x.trixnity.messenger.internal.routes.SendAttachmentRoute
import de.connect2x.trixnity.messenger.internal.routes.TimelineRoute
import de.connect2x.trixnity.messenger.internal.routes.extras.ExtrasRouteMarker
import de.connect2x.trixnity.messenger.internal.routes.extras.RoomSettingsRoute
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.SendAttachmentViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineViewModel

internal class SendAttachmentEntry(private val getRouteNavigation: GetRouteNavigation) :
    NavigationEntry<SendAttachmentRoute> {

    override fun metadata(route: SendAttachmentRoute): Map<String, Any> {
        return TwoPaneScene.right() + ThreePaneScene.middle() + timelineRouteContext(route)
    }

    @Composable
    override fun Content(route: SendAttachmentRoute) {
        val timelineViewModel = rememberComponent<TimelineViewModel>(key = TimelineStoreKey)
        val sendAttachmentViewModel = rememberComponent<SendAttachmentViewModel>()
        val routes by getRouteNavigation.routes.collectAsState()

        Column(Modifier.fillMaxSize()) {
            RoomHeader(
                roomHeaderViewModel = timelineViewModel.roomHeaderViewModel,
                showSettingsButton = showSettingsButton(routes),
                showBackButton = showBackButton(routes),
            )
            ThemedSurface(style = MaterialTheme.components.timeline) {
                SendAttachment(sendAttachmentViewModel = sendAttachmentViewModel)
            }
        }
    }

    private fun timelineRouteContext(route: SendAttachmentRoute): ComponentStoreContext {
        return ComponentStoreNavEntryDecorator.context(
            key = TimelineStoreKey,
            route = TimelineRoute(route.userId, route.roomId),
        )
    }

    private data object TimelineStoreKey : ComponentStoreKey
}

@Composable
private fun showSettingsButton(routes: List<Route>): Boolean {
    return routes.filterIsInstance<RoomSettingsRoute>().isEmpty()
}

@Composable
private fun showBackButton(routes: List<Route>): Boolean {
    return routes.filterIsInstance<ExtrasRouteMarker>().isEmpty()
}
