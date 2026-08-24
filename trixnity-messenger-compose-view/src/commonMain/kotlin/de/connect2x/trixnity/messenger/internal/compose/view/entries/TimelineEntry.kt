package de.connect2x.trixnity.messenger.internal.compose.view.entries

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import de.connect2x.trixnity.messenger.compose.view.room.timeline.InputArea
import de.connect2x.trixnity.messenger.compose.view.room.timeline.RoomHeader
import de.connect2x.trixnity.messenger.compose.view.room.timeline.Timeline
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedSurface
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.rememberComponent
import de.connect2x.trixnity.messenger.internal.compose.view.navigation.NavigationEntry
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.threepane.ThreePaneScene
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.twopane.TwoPaneScene
import de.connect2x.trixnity.messenger.internal.navigation.GetRouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.Route
import de.connect2x.trixnity.messenger.internal.routes.TimelineRoute
import de.connect2x.trixnity.messenger.internal.routes.extras.ExtrasRouteMarker
import de.connect2x.trixnity.messenger.internal.routes.extras.RoomSettingsRoute
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineViewModel

internal class TimelineEntry(private val getRouteNavigation: GetRouteNavigation) : NavigationEntry<TimelineRoute> {

    override val metadata: Map<String, Any> = TwoPaneScene.right() + ThreePaneScene.middle()

    @Composable
    override fun Content(route: TimelineRoute) {
        val timelineViewModel = rememberComponent<TimelineViewModel>()
        val routes by getRouteNavigation.routes.collectAsState()

        Column(Modifier.fillMaxSize()) {
            RoomHeader(
                roomHeaderViewModel = timelineViewModel.roomHeaderViewModel,
                showSettingsButton = showSettingsButton(routes),
                showBackButton = showBackButton(routes),
            )

            ThemedSurface(style = MaterialTheme.components.timeline) {
                Column {
                    Timeline(timelineViewModel = timelineViewModel)
                    InputArea(inputAreaViewModel = timelineViewModel.inputAreaViewModel)
                }
            }
        }
    }
}

@Composable
private fun showSettingsButton(routes: List<Route>): Boolean {
    return routes.filterIsInstance<RoomSettingsRoute>().isEmpty()
}

@Composable
private fun showBackButton(routes: List<Route>): Boolean {
    return routes.filterIsInstance<ExtrasRouteMarker>().isEmpty()
}
