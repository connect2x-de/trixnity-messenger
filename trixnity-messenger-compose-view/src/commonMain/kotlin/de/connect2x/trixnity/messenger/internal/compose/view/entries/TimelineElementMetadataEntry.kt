package de.connect2x.trixnity.messenger.internal.compose.view.entries

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.room.settings.TimelineElementMetadata
import de.connect2x.trixnity.messenger.compose.view.root.IsSinglePane
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedSurface
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.rememberComponent
import de.connect2x.trixnity.messenger.internal.compose.view.navigation.NavigationEntry
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.threepane.ThreePaneScene
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.twopane.TwoPaneScene
import de.connect2x.trixnity.messenger.internal.navigation.GetRouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.Route
import de.connect2x.trixnity.messenger.internal.routes.extras.ExtrasRouteMarker
import de.connect2x.trixnity.messenger.internal.routes.extras.TimelineElementMetadataRoute
import de.connect2x.trixnity.messenger.viewmodel.room.settings.TimelineElementMetadataViewModel

internal class TimelineElementMetadataEntry : NavigationEntry<TimelineElementMetadataRoute> {

    override val metadata: Map<String, Any> = TwoPaneScene.right() + ThreePaneScene.right()

    @Composable
    override fun Content(route: TimelineElementMetadataRoute) {
        ThemedSurface(style = MaterialTheme.components.details) {
            TimelineElementMetadata(
                viewModel = rememberComponent<TimelineElementMetadataViewModel>(),
                isBottomOfStack = rememberIsBottomOfStack<ExtrasRouteMarker>(route),
                isSinglePane = IsSinglePane.current,
            )
        }
    }
}

@Composable
private inline fun <reified SLOT : Route> rememberIsBottomOfStack(
    route: Route,
    routes: List<Route> = DI.current.get<GetRouteNavigation>().routes.collectAsState().value,
): Boolean {
    var isBottomOfStack by remember { mutableStateOf(isBottomOfStack<SLOT>(routes, route)) }

    LaunchedEffect(routes, route) {
        if (containsScreen<SLOT>(routes, route)) {
            isBottomOfStack = isBottomOfStack<SLOT>(routes, route)
        }
    }

    return isBottomOfStack
}

private inline fun <reified M : Route> findIndex(routes: List<Route>, route: Route): Int {
    val markedScreens = routes.filterIsInstance<M>()
    return markedScreens.indexOf(route)
}

private inline fun <reified M : Route> containsScreen(routes: List<Route>, route: Route): Boolean {
    return findIndex<M>(routes, route) != -1
}

private inline fun <reified M : Route> isBottomOfStack(routes: List<Route>, route: Route): Boolean {
    return findIndex<M>(routes, route) == 0
}
