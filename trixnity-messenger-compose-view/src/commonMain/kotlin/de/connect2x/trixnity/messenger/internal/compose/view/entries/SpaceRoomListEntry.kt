package de.connect2x.trixnity.messenger.internal.compose.view.entries

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import de.connect2x.trixnity.messenger.compose.view.common.LocalAccountColorsEnabled
import de.connect2x.trixnity.messenger.compose.view.space.SpaceRoomListView
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.rememberComponent
import de.connect2x.trixnity.messenger.internal.compose.view.navigation.NavigationEntry
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.NavigationVisibility
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.navigationrail.NavigationRailScene
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.threepane.ThreePaneScene
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.twopane.TwoPaneScene
import de.connect2x.trixnity.messenger.internal.routes.spaces.SpaceRoomListRoute
import de.connect2x.trixnity.messenger.viewmodel.spaces.SpaceRoomListViewModel

internal class SpaceRoomListEntry : NavigationEntry<SpaceRoomListRoute> {
    override val metadata: Map<String, Any> =
        TwoPaneScene.left() + ThreePaneScene.left() + NavigationVisibility.Always.metadata()

    override fun metadata(route: SpaceRoomListRoute): Map<String, Any> = metadata + NavigationRailScene.selection(route)

    @Composable
    override fun Content(route: SpaceRoomListRoute) {
        val viewModel = rememberComponent<SpaceRoomListViewModel>()
        val exploreSelected by viewModel.exploreSelected.collectAsState()
        CompositionLocalProvider(LocalAccountColorsEnabled provides false) {
            SpaceRoomListView(
                viewModel.content,
                viewModel.rooms,
                userId = route.userId,
                exploreSelected = exploreSelected,
                onOpenExplore = viewModel::openExplore,
            )
        }
    }
}
